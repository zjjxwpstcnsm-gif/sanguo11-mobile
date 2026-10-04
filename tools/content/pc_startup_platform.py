#!/usr/bin/env python3
"""Original CRT/context initialization with explicit isolated Win32 fixture.

No game getter, sort result, rule, event condition or effect is replaced.
"""
import struct
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP, UC_X86_REG_EAX, UC_X86_REG_EIP, UC_X86_REG_FPCW
from pc_readonly_platform import ReadOnlyPlatform


class StartupPlatform(ReadOnlyPlatform):
    def __init__(self, world, installation):
        self.extension_names = set()
        self.fibers = {}
        self.next_fiber = 0
        self.dynamic = {}
        self.pidls = {}
        self.documents = 'G:\\__fixture_documents__'
        super().__init__(world, installation)
        if (self.root / '__fixture_documents__').exists():
            raise ValueError('Virtual empty Documents fixture collides with installation')
        self.u.mem_map(0x6ee1000, 0xf000)
        self.u.mem_write(0x6ee1000, world.exe[0x6ae1000:0x6af0000])
        self.u.mem_map(world.stop + 0x1000, 0x4000)
        self.u.hook_add(UC_HOOK_CODE, self.callback,
                        begin=world.stop + 0x1000, end=world.stop + 0x4fff)
        for name, words in [('FlsAlloc', 1), ('FlsGetValue', 1),
                            ('FlsSetValue', 2), ('FlsFree', 1)]:
            self.register(name, words)
        self.import_extension(0x74e244, 'GetCurrentThreadId', 0)
        self.import_extension(0x74e158, 'IsProcessorFeaturePresent', 1)
        for slot, name, words in [(0x74e168, 'GetCPInfo', 2),
                                  (0x74e1cc, 'GetStringTypeW', 4),
                                  (0x74e1c4, 'LCMapStringW', 6),
                                  (0x74e2c4, 'CompareStringW', 6),
                                  (0x74e2d8, 'MultiByteToWideChar', 6),
                                  (0x74e310, 'WideCharToMultiByte', 8)]:
            self.import_extension(slot, name, words)
        for slot, name, words in [(0x74e3a0, 'SHGetMalloc', 1),
                                  (0x74e3a4, 'SHGetSpecialFolderLocation', 3),
                                  (0x74e3ac, 'SHGetPathFromIDList', 2)]:
            self.import_extension(slot, name, words, module='SHELL32.dll')
        free = self.register('IMalloc::Free', 2)
        release = self.register('IMalloc::Release', 1)
        vtable = self.allocate(32)
        self.u.mem_write(vtable + 8, struct.pack('<I', release))
        self.u.mem_write(vtable + 0x14, struct.pack('<I', free))
        self.shell_malloc = self.allocate(4)
        self.u.mem_write(self.shell_malloc, struct.pack('<I', vtable))
        self.callbacks[0x70c9fa] = ('calloc', 2, 0)
        self.extension_names.add('calloc')
        self.u.hook_add(UC_HOOK_CODE, self.callback, begin=0x70c9fa, end=0x70c9fa)
        if world.call(0x70e5c0) != 1:
            raise ValueError('Original CRT thread initializer failed')
        reset = world.stop + 0x180
        self.u.mem_write(reset, b'\xdb\xe3\xc3')
        world.call(reset)
        world.call(0x707075, 1)
        if self.u.reg_read(UC_X86_REG_FPCW) != 0x23f:
            raise ValueError('Original CRT x87 initialization changed')
        if world.call(0x709048, 950) != 0:
            raise ValueError('Original CRT CP950 initializer failed')
        world.call(0x4397d0, receiver=0x4ed2e48)

    def wide(self, pointer, count):
        if count == 0xffffffff:
            raw = bytes(self.u.mem_read(pointer, 8192))
            end = next(i for i in range(0, len(raw), 2) if raw[i:i + 2] == b'\0\0')
            raw = raw[:end + 2]
        else:
            if not 0 < count <= 4096:
                raise ValueError('Unexamined wide buffer length')
            raw = bytes(self.u.mem_read(pointer, count * 2))
        return raw.decode('utf-16le')

    @staticmethod
    def character_type(character):
        # Only the original CRT's masked single-byte palette is supported.
        # Other NLS classifications require an independently verified fixture.
        n = ord(character)
        if n == 0xfffd:
            return 0x200
        if n > 127:
            raise ValueError('NLS classification outside verified ASCII palette')
        flags = 0x200
        if 65 <= n <= 90: flags |= 0x101
        if 97 <= n <= 122: flags |= 0x102
        if 48 <= n <= 57: flags |= 4
        if character in ' \t\n\r\v\f': flags |= 8
        if character in ' \t': flags |= 0x40
        if n < 32 or n == 127: flags |= 0x20
        if character in '0123456789abcdefABCDEF': flags |= 0x80
        if 33 <= n <= 126 and not character.isalnum(): flags |= 0x10
        return flags

    def register(self, name, words):
        address = self.world.stop + 0x2000 + len(self.dynamic) * 16
        self.callbacks[address] = (name, words, words)
        self.extension_names.add(name)
        self.dynamic[name] = address
        return address

    def import_extension(self, slot, name, words, module='KERNEL32.dll'):
        if self.imports(self.world.exe).get(slot) != (module, name):
            raise ValueError('PE extension import identity mismatch ' + hex(slot))
        self.u.mem_write(slot, struct.pack('<I', self.register(name, words)))

    def callback(self, u, address, size, user):
        name, words, pop = self.callbacks[address]
        if name == 'GetModuleHandleA':
            sp = u.reg_read(UC_X86_REG_ESP)
            ret, pointer = struct.unpack('<2I', u.mem_read(sp, 8))
            module = self.string(pointer)
            if module.lower() not in ('kernel32', 'kernel32.dll'):
                raise ValueError('Unexamined startup module ' + module)
            self.calls.append(dict(function=name, returnPc=hex(ret), module=module, result=0x400))
            u.reg_write(UC_X86_REG_EAX, 0x400)
            u.reg_write(UC_X86_REG_ESP, sp + 8)
            u.reg_write(UC_X86_REG_EIP, ret)
            return
        if name == 'GetProcAddress':
            sp = u.reg_read(UC_X86_REG_ESP)
            ret, module, pointer = struct.unpack('<3I', u.mem_read(sp, 12))
            symbol = self.string(pointer)
            if symbol in self.dynamic:
                if module != 0x400:
                    raise ValueError('Unexamined extension module')
                value = self.dynamic[symbol]
                self.calls.append(dict(function=name, returnPc=hex(ret), symbol=symbol, result=value))
                u.reg_write(UC_X86_REG_EAX, value)
                u.reg_write(UC_X86_REG_ESP, sp + 12)
                u.reg_write(UC_X86_REG_EIP, ret)
                return
        if name not in self.extension_names:
            return super().callback(u, address, size, user)
        sp = u.reg_read(UC_X86_REG_ESP)
        stack = struct.unpack('<%dI' % (words + 1), u.mem_read(sp, (words + 1) * 4))
        ret, args = stack[0], stack[1:]
        detail = {}
        if name == 'SHGetSpecialFolderLocation':
            if args[0] != 0 or args[1] != 5:
                raise ValueError('Unexamined shell folder requested')
            pointer = self.allocate(16)
            self.pidls[pointer] = self.documents
            u.mem_write(args[2], struct.pack('<I', pointer))
            value = 0
            detail = {'virtualDocuments': self.documents, 'actualWindowsDocumentsProven': False}
        elif name == 'SHGetMalloc':
            u.mem_write(args[0], struct.pack('<I', self.shell_malloc))
            value = 0
        elif name == 'SHGetPathFromIDList':
            text = self.pidls[args[0]]
            u.mem_write(args[1], text.encode('ascii') + b'\0')
            value = 1
        elif name == 'IMalloc::Free':
            if args[0] != self.shell_malloc or args[1] not in self.pidls:
                raise ValueError('Unexamined Shell allocator release')
            self.pidls.pop(args[1])
            value = 0
        elif name == 'IMalloc::Release':
            if args[0] != self.shell_malloc:
                raise ValueError('Unexamined Shell interface')
            value = 0
        elif name == 'GetCPInfo':
            if args[0] != 950:
                raise ValueError('Unexamined code-page information request')
            # Lead ranges come from the pinned executable's own CP950 table.
            table = bytes(u.mem_read(0x8e0bc0 + 3 * 0x30, 48))
            if struct.unpack_from('<I', table)[0] != 950:
                raise ValueError('Original CRT CP950 table changed')
            raw = struct.pack('<I', 2) + b'?\0' + table[32:40] + bytes(4)
            u.mem_write(args[1], raw)
            value = 1
        elif name == 'MultiByteToWideChar':
            page, flags, pointer, count, target, capacity = args
            if page != 950 or flags not in (0, 1, 8, 9):
                raise ValueError('Unexamined multibyte conversion parameters')
            raw = bytes(u.mem_read(pointer, 8192)).split(b'\0')[0] + b'\0' if count == 0xffffffff else bytes(u.mem_read(pointer, count))
            try:
                converted = raw.decode('cp950', 'strict' if flags & 8 else 'replace').encode('utf-16le')
                value = len(converted) // 2
                if capacity:
                    if capacity < value: value = 0; self.error = 122
                    else: u.mem_write(target, converted)
            except UnicodeDecodeError:
                value = 0; self.error = 1113
            detail = {'page': page, 'flags': flags, 'inputHex': raw.hex(), 'vistaReplacementFixture': True}
        elif name == 'WideCharToMultiByte':
            page, flags, pointer, count, target, capacity, default, used = args
            if page != 950 or flags not in (0,) or default:
                raise ValueError('Unexamined wide-to-multibyte parameters')
            text = self.wide(pointer, count)
            raw = text.encode('cp950', 'replace')
            value = len(raw)
            if capacity:
                if capacity < value: value = 0; self.error = 122
                else: u.mem_write(target, raw)
            if used:
                try: text.encode('cp950'); replaced = False
                except UnicodeEncodeError: replaced = True
                u.mem_write(used, struct.pack('<I', int(replaced)))
        elif name == 'GetStringTypeW':
            kind, pointer, count, target = args
            if kind != 1: raise ValueError('Unexamined NLS classification kind')
            text = self.wide(pointer, count)
            values = [self.character_type(c) for c in text]
            u.mem_write(target, struct.pack('<%dH' % len(values), *values))
            value = 1
        elif name == 'CompareStringW':
            raise ValueError('Original Chinese sorting requires unverified Windows NLS collation; no replacement')
        elif name == 'LCMapStringW':
            locale, flags, pointer, count, target, capacity = args
            if locale not in (0, 0x404) or flags not in (0x100, 0x200):
                raise ValueError('Unexamined NLS mapping flags/locale')
            text = self.wide(pointer, count)
            if any(ord(c) > 127 and c != '\ufffd' for c in text):
                raise ValueError('NLS case mapping outside verified single-byte palette')
            raw = (text.lower() if flags == 0x100 else text.upper()).encode('utf-16le')
            value = len(raw) // 2
            if capacity:
                if capacity < value: value = 0; self.error = 122
                else: u.mem_write(target, raw)
        elif name == 'FlsAlloc':
            value = self.next_fiber
            self.next_fiber += 1
            self.fibers[value] = dict(destructor=args[0], value=0)
        elif name == 'FlsGetValue':
            value = self.fibers[args[0]]['value']
        elif name == 'FlsSetValue':
            self.fibers[args[0]]['value'] = args[1]
            value = 1
        elif name == 'FlsFree':
            self.fibers.pop(args[0])
            value = 1
        elif name == 'GetCurrentThreadId':
            value = 1
        elif name == 'IsProcessorFeaturePresent':
            if args[0] != 0:
                raise ValueError('Unexamined processor feature query')
            value = 0
            detail = {'feature': args[0], 'isolatedX87Fixture': True}
        elif name == 'calloc':
            count = args[0] * args[1]
            if count > 0x4000000:
                raise ValueError('Bounded CRT calloc overflow/arena limit')
            value = self.allocate(count)
            if count:
                u.mem_write(value, bytes(count))
        else:
            raise ValueError('Unexamined startup fixture interface ' + name)
        if name != 'calloc':
            self.calls.append(dict(function=name, returnPc=hex(ret), result=value, **detail))
        u.reg_write(UC_X86_REG_EAX, value)
        u.reg_write(UC_X86_REG_ESP, sp + 4 + pop * 4)
        u.reg_write(UC_X86_REG_EIP, ret)
