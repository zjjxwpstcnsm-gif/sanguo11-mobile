"""Read KSEF scalar programs against EXE 0x7957a0 dispatch evidence.

This preserves context offsets and curve tables. It does not assign particle
fields, clocks or gameplay causes. Unsupported instructions fail explicitly.
"""
import math
import struct
from pc_resources import sha


class UnexaminedOpcode(ValueError):
    pass


def matrix_literal_program(data,pointer,owner_end):
    """ValueType2 opcode0 copies a relative literal matrix via 4689d0/4672d0.

    Only the observed exact 96-byte layout is accepted. Matrix opcode6 is a
    different composition of matrix/vector/rotation programs and stays unknown.
    """
    size,value_type,initialized,opcode=struct.unpack_from('<4I',data,pointer)
    if size!=96 or pointer+size>owner_end or value_type!=2 or initialized or opcode!=0:
        raise UnexaminedOpcode('KSEF matrix literal layout not examined')
    child,relative,pad0,pad1=struct.unpack_from('<4I',data,pointer+16)
    target=pointer+20+relative
    if (child,relative,pad0,pad1)!=(1,12,0,0) or target+64!=pointer+size:
        raise ValueError('KSEF literal matrix pointer/padding/boundary')
    values=list(struct.unpack_from('<16f',data,target))
    if not all(math.isfinite(v)for v in values):raise ValueError('KSEF nonfinite literal matrix')
    return dict(opcode=0,operation='copy_relative_literal_matrix',child_opcode=1,
        relative_pointer_origin=pointer+20,matrix_offset=target,matrix=values,
        matrix_sha256=sha(data[target:target+64]),source='4689d0 -> 795900[1]/4672d0')


def matrix_program(data,pointer,owner_end):
    size,value_type,initialized,opcode=struct.unpack_from('<4I',data,pointer)
    if opcode==0:return matrix_literal_program(data,pointer,owner_end)
    if opcode!=6 or size not in (144,160) or pointer+size>owner_end or value_type!=2 or initialized:
        raise UnexaminedOpcode('KSEF matrix composition layout not examined')
    matrix_kind,matrix_rel,axis_kind,axis_rel,angle_kind=struct.unpack_from('<5I',data,pointer+16)
    matrix_at=pointer+20+matrix_rel;axis_at=pointer+28+axis_rel
    if (matrix_kind,axis_kind,angle_kind)!=(1,1,1) or matrix_at< pointer+40 or axis_at!=matrix_at+64 or axis_at+16!=pointer+size:
        raise ValueError('KSEF matrix/axis relative pointers and rotation expression boundary')
    # 468b50 evaluates matrix, vector4, then 79594c[1]/468ca0's scalar
    # expression to sin/cos. The scalar prefix ends where literal data begins.
    scalar_opcode=struct.unpack_from('<I',data,pointer+36)[0]
    scalar_raw=struct.pack('<4I',matrix_at-(pointer+40)+16,0,0,scalar_opcode)+data[pointer+40:matrix_at]
    angle=scalar_program(scalar_raw,0,len(scalar_raw))['program']
    matrix=list(struct.unpack_from('<16f',data,matrix_at));axis=list(struct.unpack_from('<4f',data,axis_at))
    if not all(math.isfinite(v)for v in matrix+axis):raise ValueError('KSEF nonfinite rotation literals')
    return dict(opcode=6,operation='matrix_then_source_axis_rotation',matrix_offset=matrix_at,matrix=matrix,
        matrix_sha256=sha(data[matrix_at:matrix_at+64]),axis_offset=axis_at,axis_raw_vector4=axis,
        angle_expression=angle,source='468b50 -> 468ca0 sin/cos -> 455740 rotation -> D3DX matrix multiply',
        limits='Expression/pointer boundaries only; axis semantics, source clock and matrix evaluation not yet accepted')


def scalar_program(data, pointer, owner_end):
    size, value_type, initialized, opcode = struct.unpack_from('<4I', data, pointer)
    end = pointer + size
    if size < 20 or end > owner_end or initialized != 0:
        raise ValueError('KSEF scalar record boundary/header')
    if value_type != 0:
        raise UnexaminedOpcode(f'KSEF nonscalar value type{value_type}')
    cursor = pointer + 16
    instructions = 0

    def uint():
        nonlocal cursor
        if cursor + 4 > end:
            raise ValueError('KSEF scalar program bounds')
        result = struct.unpack_from('<I', data, cursor)[0]
        cursor += 4
        return result

    def number():
        bits = uint()
        value = struct.unpack('<f', struct.pack('<I', bits))[0]
        if not math.isfinite(value):
            raise ValueError('KSEF nonfinite scalar constant')
        return value

    def count():
        n = uint()
        if n > 4096:
            raise ValueError('KSEF scalar table budget')
        return n

    def child(depth):
        return instruction(uint(), depth + 1)

    def sized_child(depth):
        words = count()
        child_end = cursor + words * 4
        result = child(depth)
        if cursor != child_end:
            raise ValueError('KSEF scalar branch byte count')
        return result

    def instruction(op, depth):
        nonlocal instructions
        instructions += 1
        if instructions > 4096 or depth > 64:
            raise ValueError('KSEF scalar recursion budget')
        node = {'opcode': op}
        if op == 0:
            node.update(operation='constant', value=number())
        elif op == 1:
            node.update(operation='context_float', byte_offset=uint())
        elif op == 45:
            node.update(operation='context_pointer_then_scalar',byte_offset=uint(),program=child(depth),
                        source='466710 loads pointer from context+offset; nested scalar uses pointed context')
        elif op == 46:
            source_input=child(depth);scale=number();n=count()
            if n<1:raise ValueError('Unexamined source harmonic zero count')
            node.update(operation='source_trigonometric_series_raw',input=source_input,scale=scale,
                        term_count=n,cosine_coefficient=number(),sine_coefficient=number(),
                        source='468080 sin/cos then repeated double-angle/half-weight accumulation',
                        limits='Structure only; recurrence/f32 and actual context clock not yet evaluated')
        elif op == 2:
            node.update(operation='visual_uniform_midpoint_radius', midpoint=number(), radius=number(),
                        rng='PC calls0x444280; Android gameplay RNG must never be used')
        elif op in (3, 4):
            node.update(operation='source_lookup_unresolved', input=child(depth), scale=number(), lookup=uint())
        elif op == 5:
            node.update(operation='compose_single_float_context', input=child(depth), program=child(depth))
        elif op == 6:
            n = count()
            if n > 8:
                raise ValueError('PC local context stack capacity')
            node.update(operation='compose_float_context', inputs=[child(depth) for _ in range(n)], program=child(depth))
        elif op in (7, 21, 22, 23, 24, 25, 26):
            node.update(operation={7:'negate', 21:'sqrt', 22:'sin', 23:'cos', 24:'tan', 25:'exp', 26:'log'}[op], input=child(depth))
        elif op in (10, 12, 13, 15):
            node.update(operation={10:'add', 12:'subtract', 13:'multiply', 15:'divide'}[op], left=child(depth), right=child(depth))
        elif op in (11, 14):
            node.update(operation='add_literal' if op == 11 else 'multiply_literal', input=child(depth), value=number())
        elif op in (16, 17):
            n = count()
            if n < 2:
                # The PC routine's n<=1 stack return is not inferred as a
                # convenient identity operation in the converter.
                raise ValueError('Unexamined PC variadic edge count')
            node.update(operation='sum' if op == 16 else 'product', inputs=[child(depth) for _ in range(n)])
        elif op in (18, 19):
            node.update(operation='square' if op == 18 else 'cube', input=child(depth))
        elif op == 20:
            n = count()
            if n < 1:
                raise ValueError('KSEF polynomial coefficient count')
            node.update(operation='polynomial_ascending', input=child(depth), coefficients=[number() for _ in range(n)])
        elif op == 27:
            node.update(operation='nonnegative_branch_raw', input=child(depth), program=sized_child(depth),
                        limits='selection/zero boundary not yet evaluated')
        elif op == 28:
            node.update(operation='threshold_branches_raw', input=child(depth))
            n = count()
            # The second count includes all threshold/value/default words.
            words = count()
            table_end = cursor + words * 4
            node['branches'] = [{'threshold':number(), 'program':sized_child(depth)} for _ in range(n)]
            node['default'] = child(depth)
            if cursor != table_end:
                raise ValueError('KSEF threshold table boundary')
        elif op in (30, 31):
            node.update(operation='linear_keys_raw' if op == 30 else 'cubic_keys_raw', input=child(depth))
            n = count()
            node['interval_count_raw'] = n
            node['values_raw'] = [number() for _ in range(n * 2 if op == 30 else n * 5 + 2)]
            node['limits'] = 'Source table bounds only; interpolation and context time assignment not accepted yet'
        elif op in (32, 33, 34):
            node.update(operation='source_context_ratio_raw',
                        byte_offsets=[uint() for _ in range({32:3, 33:5, 34:7}[op])],
                        limits='EXE context loads confirmed; field meanings and denominator edge behavior pending')
        elif op in (35, 37):
            node.update(operation='minimum' if op == 35 else 'maximum',
                        left=child(depth), right=child(depth))
        elif op in (36, 38):
            node.update(operation='minimum_literal' if op == 36 else 'maximum_literal',
                        input=child(depth), value=number())
        else:
            raise UnexaminedOpcode(f'KSEF scalar opcode{op}')
        return node

    program = instruction(opcode, 0)
    if cursor != end:
        raise ValueError(f'KSEF scalar unexplained bytes {end-cursor}')
    return {'offset':pointer, 'bytes':size, 'sha256':sha(data[pointer:end]), 'instructions':instructions,
            'program':program, 'runtime_binding':None}
