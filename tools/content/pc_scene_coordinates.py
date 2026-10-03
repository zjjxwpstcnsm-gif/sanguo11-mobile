"""Uniform PC visual coordinates established from the supplied executable.

415920 returns heightByte * .5; 41c356 stores OBJS x/z * 10 and y * .5.
The scene horizontal transform already used PC units * .05. Use the same
factor on Y instead of the former .02 flattening. PC camera/raster acceptance
is separate from this arithmetic contract; no source files are written.
"""
WORLD_SCALE = .05
WORLD_AXES = [WORLD_SCALE] * 3
HEIGHT_BYTE_SCALE = .5 * WORLD_SCALE


def contract(binary_format):
    return dict(source_world_to_scene_axes=WORLD_AXES,
                height_byte_to_scene=HEIGHT_BYTE_SCALE,
                binary_format=binary_format,
                source_machine_proof='tools/content/check_pc_coordinates.py',
                raw_vertex_crosscheck='tools/content/check_pc_static_coordinates.py',
                status='Converted source coordinates; installed and PC camera/lighting/order comparison tracked separately')
