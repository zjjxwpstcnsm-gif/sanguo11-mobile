"""Bounded KSEF controller records, distinct from spawn scalar expressions.

Dispatches are confirmed in the supplied EXE at 0x794cf0 and 0x794f28.
Context offsets retain their original numbers: no inferred alpha/size roles,
gameplay causes, duration units, material bindings or Android effects here.
"""
import math
import struct
from pc_effect_curves import scalar_program, UnexaminedOpcode
from pc_resources import sha


def controller(data, pointer, owner_end, depth=0):
    if depth > 32 or pointer < 0 or pointer + 16 > owner_end or owner_end > len(data):
        raise ValueError('KSEF controller bounds/depth')
    size, kind, output, initialized = struct.unpack_from('<4I', data, pointer)
    end = pointer + size
    if size < 20 or size % 4 or end > owner_end or initialized:
        raise ValueError('KSEF controller record boundary/state')

    def uint(offset):
        if pointer + offset + 4 > end:
            raise ValueError('KSEF controller field bounds')
        return struct.unpack_from('<I', data, pointer + offset)[0]

    def number(offset):
        value = struct.unpack('<f', struct.pack('<I', uint(offset)))[0]
        if not math.isfinite(value):
            raise ValueError('KSEF nonfinite controller parameter')
        return value

    result = dict(offset=pointer, bytes=size, sha256=sha(data[pointer:end]),
                  controller_kind=kind, output_byte_offset_raw=output,
                  runtime_binding=None)
    if kind in (0, 1, 2, 3, 9, 10, 11, 12):
        result.update(input_byte_offset_raw=uint(0x10),
                      normalization_byte_offset_raw=uint(0x14),
                      multiplier_byte_offset_raw=uint(0x18),
                      parameter_raw=number(0x1c))
        nested = pointer + 0x20
        if nested + 16 > end:
            raise ValueError('KSEF interpolator header bounds')
        length, interpolation, classification, relocated = struct.unpack_from('<4I', data, nested)
        expected = 32 if interpolation in (0, 1, 2) else 44 if 3 <= interpolation <= 11 else None
        if expected is None:
            raise UnexaminedOpcode(f'KSEF interpolator type{interpolation}')
        if length != expected or nested + length > end or relocated:
            raise ValueError('KSEF interpolator boundary/state')
        values = [number(0x30 + i * 4) for i in range((length - 16) // 4)]
        result['interpolator'] = dict(offset=nested, bytes=length, type=interpolation,
                                     classification_raw=classification, parameters_raw=values,
                                     operation={0:'clamped-linear', 1:'stateful-relaxation',
                                                2:'clamped-smoothstep', 3:'linear-hold-linear',
                                                4:'linear-hold-stateful-relaxation'}.get(interpolation, 'unresolved'))
    elif kind in (4, 6, 17, 18, 19, 20, 26):
        result['expression'] = scalar_program(data, pointer + 0x10, end)
        result['operation'] = 'expression-assignment' if kind == 4 else 'expression-update-unresolved'
    elif kind in (7, 8, 13, 21, 22, 23, 24, 25):
        result['nested'] = controller(data, pointer + 0x10, end, depth + 1)
        result['operation'] = 'float-to-byte' if kind in (7, 8) else 'nested-context-update-unresolved'
    elif kind in (14, 15, 16):
        count = uint(0x1c)
        if count > 4096 or 0x20 + count * 8 > size:
            raise ValueError('KSEF controller impulse table bounds')
        result.update(operation='time-window-impulse-accumulation',
                      input_byte_offset_raw=uint(0x10),
                      multiplier_byte_offset_raw=uint(0x14),
                      period_raw=number(0x18),
                      impulses=[dict(time_raw=number(0x20+i*8), value=number(0x24+i*8)) for i in range(count)])
        # Type14 is nonperiodic. Type15/16 reduce the input by floor(t/period)
        # and process wrap-around. Neither is an opacity keyframe table.
        result['periodic'] = kind in (15, 16)
    elif kind == 27:
        if size != 20 or uint(0x10):
            raise ValueError('KSEF parent-linked byte controller boundary/state')
        result['operation'] = 'parent-linked-float-to-byte'
        result['limits'] = 'Runtime parent +0xcc and +0x24/+0x38 supply values; unresolved context ownership'
    else:
        raise UnexaminedOpcode(f'KSEF controller type{kind}')
    return result
