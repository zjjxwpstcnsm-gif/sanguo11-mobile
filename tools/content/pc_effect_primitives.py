"""Source primitive2 local quad, confirmed at EXE442230.

Payload addresses are relative to serialized default-context+10, not the
enclosing runtime instance. Scene placement/emission are separate contracts.
"""
import math
import struct


def quad(width,height,tint_bgra,rectangle):
    if len(rectangle)!=4 or not all(math.isfinite(x)for x in (width,height,*rectangle)):
        raise ValueError('Source quad nonfinite dimensions/UV')
    if not 0<=tint_bgra<=0xffffffff:raise ValueError('Source quad color')
    # Source f32 dimensions multiplied by exact .5, stored as f32 before use.
    half_x=struct.unpack('<f',struct.pack('<f',width*.5))[0]
    half_y=struct.unpack('<f',struct.pack('<f',height*.5))[0]
    left,top,right,bottom=rectangle
    return b''.join(struct.pack('<3fI2f',x,y,0.0,tint_bgra,u,v)for x,y,u,v in (
        (-half_x,half_y,left,top),(half_x,half_y,right,top),
        (-half_x,-half_y,left,bottom),(half_x,-half_y,right,bottom)))
