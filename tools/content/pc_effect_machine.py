"""Read-only KSEF source execution oracle, isolated from gameplay and Android.

Only heap/CRT allocation and three named Windows queries are adapted. The
original constructors, pointer relocation, controllers, emission, matrix math,
visual random generator, particle arena and updates execute supplied EXE bytes.
This is investigation evidence, not an Android emitter or PC image reference.
"""
import collections
import struct

from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import (UC_X86_REG_ESP, UC_X86_REG_ECX, UC_X86_REG_ESI,
                               UC_X86_REG_EAX, UC_X86_REG_EIP, UC_X86_REG_FPCW)

from inspect_pc_effect_bindings import EXE_SHA
from pc_resources import sha


class VerifiedSourceExecutable:
    """One immutable, hash-checked supplied EXE for a bounded resource sweep.
    Reusing bytes avoids hashing the same160MB EXE for every independent VM.
    """
    __slots__ = ('_bytes',)
    def __init__(self, data):
        if not isinstance(data, bytes) or sha(data) != EXE_SHA:
            raise ValueError('Source EXE differs: re-inspect the oracle')
        object.__setattr__(self, '_bytes', data)
    def __setattr__(self, name, value):
        raise AttributeError('Verified source executable is immutable')
    @property
    def data(self):return self._bytes


class SourceEffectMachine:
    HEAP, STACK, STOP = 0x10000000, 0x20000000, 0x30000000
    PLATFORM = ((0x74e008, 'RegOpenKeyA', 2, 3),
                (0x74e21c, 'GetModuleHandleA', 0, 1),
                (0x74e158, 'IsProcessorFeaturePresent', 0, 1))
    MEMORY = (0x4438d0, 0x443930, 0x443950, 0x71deb8, 0x707dff)
    WATCH = (0x457c40, 0x465a10, 0x4657b0, 0x45aed0, 0x45cf90,
             0x464bc0, 0x444280, 0x4442a0, 0x46d400)

    def __init__(self, exe, instruction_budget=5_000_000):
        if isinstance(exe, VerifiedSourceExecutable):exe=exe.data
        else:exe=VerifiedSourceExecutable(exe).data
        # These function table writers really occur in the native CRT list.
        if struct.unpack_from('<2I', exe, 0x49d0a4) != (0x73bc80, 0x73bd20):
            raise ValueError('Native CRT initializer table changed')
        if struct.unpack_from('<3I', exe, 0x4a5b68) != (625, 0, 0x9908b0df):
            raise ValueError('Native visual MT startup changed')
        self.u = Uc(UC_ARCH_X86, UC_MODE_32)
        self.u.mem_map(0, 4096)  # Native FS[0] structured-exception chain.
        self.u.mem_map(0x400000, 0x500000)
        self.u.mem_write(0x400000, exe[:0x500000])
        self.u.mem_map(0x6ed0000, 0x10000)
        self.u.mem_map(0x9c00000, 0x100000)
        self.u.mem_write(0x9c00000, exe[0x9800000:0x9900000])
        self.u.mem_map(self.HEAP, 0x1000000)
        self.u.mem_map(self.STACK, 0x10000)
        self.u.mem_map(self.STOP, 4096)
        for i, (iat, name, value, argc) in enumerate(self.PLATFORM):
            import_offset = struct.unpack_from('<I', exe, iat-0x400000)[0]
            imported = exe[import_offset+2:].split(b'\0', 1)[0].decode('ascii')
            if imported != name:
                raise ValueError(('Windows import changed', hex(iat), imported))
            adapter = self.STOP+0x100+i*16
            self.u.mem_write(adapter, b'\xb8'+struct.pack('<I', value)
                             +b'\xc2'+struct.pack('<H', argc*4))
            self.u.mem_write(iat, struct.pack('<I', adapter))
        self.instruction_budget = instruction_budget
        self.next_alloc = self.HEAP+0x100000
        self.allocations = []
        self.calls = collections.Counter()
        self.trace = collections.deque(maxlen=70)
        self.attempts = self.successes = 0
        self.particle_owners = {}
        self._code_hook = self.u.hook_add(UC_HOOK_CODE, self._hook)
        # Unicorn starts with CW=0, which is not the initialized processor
        # environment. Execute FNINIT as an explicit CPU-reset input, then the
        # original CRT startup707075(1), including its precision setup70da68.
        # Do not swallow RaiseException or replace the original math routines.
        reset = self.STOP+0x180
        self.u.mem_write(reset, b'\xdb\xe3\xc3')
        self.call(reset, 0)
        self.call(0x707075, 0, 1)
        if self.u.reg_read(UC_X86_REG_FPCW) != 0x23f:
            raise ValueError('Original CRT x87 state '+hex(self.u.reg_read(UC_X86_REG_FPCW)))

    def close(self):
        # Break the Python callback/Uc ownership cycle between resource probes.
        if self.u is not None:
            self.u.hook_del(self._code_hook)
            self.u = None

    def uint(self, address):
        return struct.unpack('<I', self.u.mem_read(address, 4))[0]

    def _hook(self, machine, address, size, user):
        self.trace.append(hex(address))
        if address < 0x400000:
            raise RuntimeError('Unresolved non-source code entry '+hex(address))
        if address in self.WATCH or address in self.MEMORY:
            self.calls[hex(address)] += 1
        # Real 45cec0 has just returned a particle pointer into the source arena.
        if address == 0x45cf99:
            self.attempts += 1
            if machine.reg_read(UC_X86_REG_EAX):
                self.successes += 1
                self.particle_owners[machine.reg_read(UC_X86_REG_EAX)] = machine.reg_read(UC_X86_REG_ESI)
        if address not in self.MEMORY:
            return
        sp = machine.reg_read(UC_X86_REG_ESP)
        ret = self.uint(sp)
        a, b, c = struct.unpack('<3I', machine.mem_read(sp+4, 12))
        if address == 0x4438d0:
            length, alignment = c, b
        elif address == 0x443930:
            length, alignment = b, 16
        elif address == 0x71deb8:
            length, alignment = max(16, a), 16
        else:
            length, alignment = 0, 16
        if length:
            if alignment < 1 or alignment & (alignment-1) or length > 0x800000:
                raise ValueError('Unexamined source allocation request')
            self.next_alloc = (self.next_alloc+alignment-1)//alignment*alignment
            pointer = self.next_alloc
            self.next_alloc += length
            if self.next_alloc > self.HEAP+0x1000000:
                raise MemoryError('Bounded oracle heap exhausted')
            # A deterministic zeroed allocation boundary, not a claim about
            # indeterminate Windows heap bytes. Native setup writes live state.
            machine.mem_write(pointer, b'\0'*length)
            self.allocations.append(dict(function=hex(address), pointer=pointer,
                                         bytes=length, args=[a, b, c]))
            machine.reg_write(UC_X86_REG_EAX, pointer)
        machine.reg_write(UC_X86_REG_ESP, sp+4)
        machine.reg_write(UC_X86_REG_EIP, ret)

    def call(self, address, this, *args):
        self.u.reg_write(UC_X86_REG_ESP, self.STACK+0x8000)
        self.u.reg_write(UC_X86_REG_ECX, this)
        self.u.mem_write(self.STACK+0x8000,
                         struct.pack('<'+'I'*(len(args)+1), self.STOP, *args))
        self.u.emu_start(address, self.STOP, count=self.instruction_budget)
        if self.u.reg_read(UC_X86_REG_EIP) != self.STOP:
            raise RuntimeError('Source execution instruction budget exhausted')
        return self.u.reg_read(UC_X86_REG_EAX)

    def load(self, raw, scene_camera_provider=None):
        if not raw.startswith(b'KSEF0131') or len(raw) > 0xe0000:
            raise ValueError('Bounded source KSEF input')
        self.instance, self.data = self.HEAP, self.HEAP+0x1000
        self.u.mem_write(self.data, raw)
        for address in (0x73bc80, 0x73bd20):
            self.call(address, 0)
        if self.call(0x45b710, 0, 0, 0, 0x400000) != 1:
            raise ValueError('Original 4 MiB particle arena failed')
        self.call(0x457c90, self.instance)
        if scene_camera_provider is not None:
            if self.uint(scene_camera_provider)!=0x795190:
                raise ValueError('Unexamined native scene camera provider')
            # Original45a42b/45a436 binds the parent's provider BEFORE loading
            # child transforms. Their initialized slot1 references are cached.
            self.call(0x457bd0,self.instance,1,scene_camera_provider)
        end = self.call(0x457dd0, self.instance, self.data+20, 0, 0)
        if end != self.data+len(raw):
            raise ValueError('Source loader did not consume exact KSEF boundary')
        self.call(0x457900, self.instance)

    def start(self, matrix=None):
        # 413d20 ->457880: source count1/matrix packet, not a fabricated emitter.
        pointer=self.data+20
        if matrix is not None:
            if not isinstance(matrix,bytes) or len(matrix)!=64:
                raise ValueError('Native scene start matrix boundary')
            import math
            if not all(math.isfinite(v)for v in struct.unpack('<16f',matrix)):
                raise ValueError('Native scene start matrix finite input')
            pointer=self.STOP+0x400;self.u.mem_write(pointer,matrix)
        return self.call(0x457880, self.instance, 1, pointer)

    def update(self, dt):
        return self.call(0x457a20, self.instance,
                         struct.unpack('<I', struct.pack('<f', dt))[0])

    def snapshot(self):
        block = bytes(self.u.mem_read(self.instance, 0xb0))
        return dict(root_state_sha256=sha(block), root_elapsed=struct.unpack_from('<f', block, 4)[0],
                    source_emission_attempts=self.attempts, source_particle_allocations=self.successes,
                    memory_allocation_count=len(self.allocations),
                    memory_allocation_bytes=sum(x['bytes'] for x in self.allocations),
                    source_calls=dict(self.calls))

    def particle_storage(self):
        """Original allocator slots, including expired/reused slots. No invented
        alive flags or XYZ/color semantics; decode these against source consumers.
        Native virtual size accessor executes rather than guessing record length.
        """
        sizes = {}
        pools = {}
        records = []
        for pointer, owner in sorted(self.particle_owners.items()):
            if owner not in sizes:
                table = self.uint(owner+0x2c)
                accessor = self.uint(table+0x14)
                if not 0x401000 <= accessor < 0x74d000:
                    raise ValueError('Unexamined source particle size accessor')
                length = self.call(accessor, owner)
                if not 0 < length <= 4096:
                    raise ValueError('Unexamined source particle stride')
                sizes[owner] = length
                pool = self.uint(owner+0x1c)
                chain = []
                while pool:
                    if len(chain)>4096 or any(item[0]==pool for item in chain):
                        raise ValueError('Source pool chain cycle/budget')
                    shift = bytes(self.u.mem_read(pool+1,1))[0]
                    if shift>31:raise ValueError('Source pool shift')
                    extent = self.uint(pool+4)>>shift
                    active = set();node = self.uint(pool+0x1c)
                    while node:
                        if node in active or len(active)>4096:raise ValueError('Source active slot cycle/budget')
                        active.add(node);node = self.uint(node+0xc)
                    chain.append((pool,extent,active));pool = self.uint(pool+0x14)
                pools[owner] = chain
            length = sizes[owner]
            match = next((item for item in pools[owner] if item[0]+0x20<=pointer and pointer+length<=item[0]+item[1]),None)
            if match is None:raise ValueError('Source slot absent from original pool chain')
            pool,extent,active=match;base=pool+0x20
            if (pointer-base)%length:raise ValueError('Source slot stride misalignment')
            raw = bytes(self.u.mem_read(pointer, length))
            records.append(dict(owner=hex(owner), slot=(pointer-base)//length,
                pool=hex(pool),pool_bytes=extent,allocator_active=pointer in active,
                pointer=hex(pointer), bytes=length, sha256=sha(raw), raw_hex=raw.hex()))
        return records
