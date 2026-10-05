"""Source KSEF leaf UV programs, not an effect emitter or gameplay binding.

EXE 46d670 selects five-function tables at 7961a8, stride 20. Leaf
45d280 updates the UV context at +50, then copies its rectangle to +30.
No random numbers, guessed durations or platform clock enter this decoder.
"""
import math
import struct
from pc_resources import sha


def decode_uv(data, owner, owner_bytes):
    if owner < 0 or owner_bytes < 0x130 or owner + owner_bytes > len(data):
        raise ValueError('KSEF UV owner bounds')
    relative = struct.unpack_from('<I', data, owner + 0x110)[0]
    if not relative:
        return None
    if relative < 0x130 or relative + 16 > owner_bytes:
        raise ValueError('KSEF UV header bounds')
    offset = owner + relative
    kind, initialized, parameter_bits, count = struct.unpack_from('<4I', data, offset)
    if kind not in (0, 1, 2, 3) or initialized:
        raise ValueError('Unexamined KSEF UV dispatch/state')
    if not 0 < count <= 4096 or relative + 16 + count * 16 > owner_bytes:
        raise ValueError('KSEF UV rectangle count/bounds')
    rects = [list(row) for row in struct.iter_unpack('<4f', data[offset+16:offset+16+count*16])]
    if not all(math.isfinite(v) for row in rects for v in row):
        raise ValueError('KSEF UV nonfinite rectangle')
    parameter = None if kind == 0 else struct.unpack('<f', struct.pack('<I', parameter_bits))[0] if kind < 3 else parameter_bits
    if kind in (1, 2) and (not math.isfinite(parameter) or parameter < 0):
        raise ValueError('KSEF UV time multiplier')
    if kind == 3 and not 0 < parameter <= 4096:
        raise ValueError('KSEF UV repeat divisor')
    size = 16 + count * 16
    return dict(offset=offset, bytes=size, sha256=sha(data[offset:offset+size]), kind=kind,
                raw_parameter_bits=parameter_bits, parameter=parameter, rectangles=rects,
                mode=('static-start-index', 'elapsed-time-loop', 'lifetime-ratio-loop',
                      'one-step-per-update-repeated-rectangles')[kind],
                time_units='Source instance elapsed/lifetime floats; PC wall-clock conversion unverified',
                runtime_binding=None)


def sample_uv(program, *, initial_index, elapsed, lifetime=None, updates=None):
    """Evaluate examined source UV formulas with caller-supplied source context.

    The PC conversion helper 707a74 truncates toward zero. Source unsigned
    division uses the low u32 result, including overflow. Kind3 depends on
    update count, so it must never be changed to an invented FPS multiplier.
    This diagnostic function does not determine an effect's lifetime.
    """
    if not isinstance(initial_index, int) or not 0 <= initial_index <= 0xffffffff:
        raise ValueError('KSEF UV initial index')
    if not math.isfinite(elapsed) or elapsed < 0:
        raise ValueError('KSEF UV elapsed source time')
    kind = program['kind']; count = len(program['rectangles'])
    if kind == 3:
        if not isinstance(updates, int) or updates < 0:
            raise ValueError('KSEF UV type3 requires actual update count')
        repeat = program['parameter']
        start = ((repeat * initial_index) & 0xffffffff) % (repeat * count)
        index = (start + updates) % (repeat * count)
        index //= repeat
    else:
        if kind == 2:
            if lifetime is None or not math.isfinite(lifetime) or lifetime <= 0:
                raise ValueError('KSEF UV type2 requires source lifetime')
            time = elapsed / lifetime
        else:
            time = elapsed
        advance = 0 if kind == 0 else math.trunc(time * program['parameter'])
        index = (((initial_index % count) + advance) & 0xffffffff) % count
    return index, program['rectangles'][index]
