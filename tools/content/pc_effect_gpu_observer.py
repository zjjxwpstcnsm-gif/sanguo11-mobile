"""Observe exact source final quad writes at four named GPU memory boundaries.

Executes original0/1/2/3 primitive functions and441a40 camera basis builder.
Texture/blend setup, queue depth ordering and GPU rasterization remain separate.
"""
import struct
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_EDX, UC_X86_REG_EDI, UC_X86_REG_ESI, UC_X86_REG_ESP, UC_X86_REG_EIP
from pc_effect_primitives import quad


class FinalQuadObserver:
    def __init__(self, machine, camera, materials=False, preserve_ring=False):
        self.machine = machine
        self.base = 0x31000000  # DrawPacketObserver has already mapped this page range.
        self.device = self.base+0x3000
        self.buffer = self.base+0x4000
        self.vertices = self.base+0x8000
        self.frame = self.base+0x9000
        self.payload = self.base+0xa000
        self.unlock, self.draw = machine.STOP+0x300, machine.STOP+0x320
        self.materials = materials
        self.preserve_ring = preserve_ring
        self.material_state = dict(texture_index=None, blend_operation=None, blend_source=None, blend_destination=None)
        u = machine.u
        device_table, buffer_table = self.base+0x5000, self.base+0x6000
        for address, value in ((self.device, device_table), (self.buffer, buffer_table),
                               (device_table+0x144, self.draw), (buffer_table+0x30, self.unlock),
                               (0x6ed6f74, self.device), (0x6ed37dc, self.buffer)):
            u.mem_write(address, struct.pack('<I', value))
        machine.call(0x441a40, 0, camera, self.frame+0x10)
        if materials:
            library, records = self.base+0xb000, self.base+0xb100
            u.mem_write(library, struct.pack('<I', records))
            u.mem_write(library+0xc, struct.pack('<H', 33))
            u.mem_write(library+0x10, struct.pack('<H', 0xffff))
            for i in range(33): u.mem_write(records+i*16, struct.pack('<I', i+1))
            u.mem_write(self.frame+4, struct.pack('<I', library))
            u.mem_write(self.frame+0xc, struct.pack('<H', 5))
            u.mem_write(device_table+0x104, struct.pack('<I', machine.STOP+0x340))
            u.mem_write(device_table+0xe4, struct.pack('<I', machine.STOP+0x360))
        self.matrix_writes = []
        self.draws, self.locks = [], []
        self.hook = u.hook_add(UC_HOOK_CODE, self._record)

    def _record(self, u, address, size, user):
        if address not in (0x44d560, 0x44d160, self.unlock, self.draw, self.machine.STOP+0x340, self.machine.STOP+0x360): return
        sp = u.reg_read(UC_X86_REG_ESP)
        ret = self.machine.uint(sp)
        pop = 0
        if address in (self.machine.STOP+0x340, self.machine.STOP+0x360):
            device, key, value = struct.unpack('<3I', u.mem_read(sp+4, 12))
            if device != self.device: raise ValueError('Material source device')
            if address == self.machine.STOP+0x340:
                if key != 0 or not 1 <= value <= 33: raise ValueError('Source library33 image GPU handle')
                self.material_state['texture_index'] = value-1
            else:
                field = {171:'blend_operation', 19:'blend_source', 20:'blend_destination'}.get(key)
                if field is None: raise ValueError('Unexamined source material state')
                self.material_state[field] = value
            pop = 12
        elif address == 0x44d560:
            device, index, source = struct.unpack('<3I', u.mem_read(sp+4, 12))
            if device != self.device or index != 30: raise ValueError('Unexamined quad shader constant')
            self.matrix_writes.append(bytes(u.mem_read(source, 64)))
        elif address == 0x44d160:
            buffer, offset, length, destination, flags = struct.unpack('<5I', u.mem_read(sp+4, 20))
            if buffer != self.buffer or length != 96: raise ValueError('Unexamined quad buffer lock')
            self.locks.append((offset, length, flags))
            u.mem_write(destination, struct.pack('<I', self.vertices))
        elif address == self.unlock:
            pop = 4
        else:
            device, primitive, first, count = struct.unpack('<4I', u.mem_read(sp+4, 16))
            if device != self.device: raise ValueError('Unexamined quad device')
            self.draws.append((primitive, first, count))
            pop = 16
        u.reg_write(UC_X86_REG_EAX, 0)
        u.reg_write(UC_X86_REG_ESP, sp+4+pop)
        u.reg_write(UC_X86_REG_EIP, ret)

    def evaluate(self, packet):
        primitive = packet['primitive']
        if primitive not in (0, 1, 2, 3): raise ValueError('Only examined final quad primitives0..3')
        raw = bytes.fromhex(packet['raw_hex'])
        if len(raw) != 0x70: raise ValueError('Quad prefix boundary')
        u, payload = self.machine.u, self.payload
        u.mem_write(payload, raw)
        if not self.preserve_ring: u.mem_write(0x8a5b64, struct.pack('<I', 0))
        self.matrix_writes.clear(); self.draws.clear(); self.locks.clear()
        if self.materials:
            self.machine.call(0x442c00, 0, self.frame, payload)
        elif primitive == 0:
            u.reg_write(UC_X86_REG_EAX, payload+0x30)
            u.reg_write(UC_X86_REG_EDX, self.frame)
            self.machine.call(0x4423b0, 0, payload)
        elif primitive == 1:
            u.reg_write(UC_X86_REG_EAX, payload)
            u.reg_write(UC_X86_REG_EDI, payload+0x30)
            self.machine.call(0x442400, 0, self.frame)
        elif primitive == 2:
            u.reg_write(UC_X86_REG_EAX, payload+0x30)
            u.reg_write(UC_X86_REG_ESI, payload)
            self.machine.call(0x442230, 0)
        else:
            u.reg_write(UC_X86_REG_EAX, self.frame)
            u.reg_write(UC_X86_REG_EDI, payload+0x30)
            self.machine.call(0x442480, payload)
        vertices = bytes(u.mem_read(self.vertices, 96))
        expected = quad(*struct.unpack_from('<2f', raw, 0x14),
                        struct.unpack_from('<I', raw, 0x10)[0], struct.unpack_from('<4f', raw, 0x20))
        if vertices != expected: raise AssertionError('Original96-byte final quad differs from decoded BGRA/UV/dimensions')
        if len(self.locks) != 1: raise AssertionError('Source single final quad lock')
        offset, length, flags = self.locks[0]
        if offset % 24 or not 0 <= offset <= 0xc000-96 or length != 96 or flags not in (0x1000, 0x2000):
            raise AssertionError('Source dynamic ring buffer bounds')
        if len(self.matrix_writes) != 1 or self.draws != [(5, offset//24, 2)]:
            raise AssertionError('Final matrix/strip/dynamic lock contract')
        if not self.preserve_ring and self.locks != [(0, 96, 0x1000)]: raise AssertionError('Isolated quad lock contract')
        result = dict(quad_vb_hex=vertices.hex(), quad_matrix_hex=self.matrix_writes[0].hex())
        if self.materials:
            if any(v is None for v in self.material_state.values()): raise ValueError('Incomplete source material state')
            result.update(self.material_state)
        if self.preserve_ring: result.update(vertex_offset=offset, lock_flags=flags, draw_first=offset//24)
        return result

    def close(self):
        self.machine.u.hook_del(self.hook)
