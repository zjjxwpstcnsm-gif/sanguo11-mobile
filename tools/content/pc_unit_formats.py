"""Strict source map-unit WKMD/FCVD readers, independent of event-character rigs.

Source math is row-vector D3D: local * parent; inverse bind * animated global.
No source curve resampling, bone renaming, pose fabrication or geometry reduction.
"""
import bisect
import math
import struct
import numpy as np


def nested_link(data):
    if data[:4] != b'LINK' or len(data) < 16:
        raise ValueError('motion LINK header')
    count, unit, reserved = struct.unpack_from('<3I', data, 4)
    if count != 75 or unit != 1 or reserved:
        raise ValueError('map motion LINK contract')
    result, end = [], 16 + count * 8
    for offset, length in struct.iter_unpack('<2I', data[16:end]):
        if offset != end or length < 16 or offset + length > len(data):
            raise ValueError('motion LINK non-contiguous/truncated entry')
        result.append(data[offset:offset + length])
        end = offset + length
    if end != len(data):
        raise ValueError('motion LINK trailing bytes')
    return result


def fcvd(data):
    if data[:8] != b'FCVD0022' or len(data) < 16:
        raise ValueError('FCVD map motion header')
    units, frames, count = struct.unpack_from('<IHH', data, 8)
    if units * 16 != len(data) or not 2 <= frames <= 1000 or not 7 <= count <= 1000:
        raise ValueError('FCVD length/count')
    channels, seen = [], set()
    end = (16 + count * 4 + 15) // 16 * 16
    for offset16 in struct.unpack_from('<' + 'I' * count, data, 16):
        offset = offset16 * 16
        if offset != end or offset + 32 > len(data):
            raise ValueError('FCVD channel offset')
        size, encoding, kind, node, keys, reserved = struct.unpack_from('<IBBHII', data, offset)
        if encoding != 3 or kind not in (6, 7, 8, 34, 35, 36, 37) or node > 127 or reserved or not 1 <= keys <= frames:
            raise ValueError('FCVD channel contract')
        if size != keys + 1 or offset + size * 16 > len(data) or (node, kind) in seen:
            raise ValueError('FCVD keys/duplicate')
        seen.add((node, kind))
        values, previous = [], -1
        for words, upper, a, b, c in struct.iter_unpack('<HH3f', data[offset + 16:offset + size * 16]):
            if words != 4 or not previous < upper < frames or not all(math.isfinite(v) for v in (a,b,c)):
                raise ValueError('FCVD keyframe contract')
            values.append((upper, a, b, c)); previous = upper
        if previous != frames - 1:
            raise ValueError('FCVD channel does not cover duration')
        channels.append(dict(node=node, kind=kind, keys=values))
        end = offset + size * 16
    nodes = max(c['node'] for c in channels) + 1
    if len(seen) != nodes * 7 or end != len(data):
        raise ValueError('FCVD incomplete pose/trailing bytes')
    return dict(frames=frames, nodes=nodes, channels=channels)


def evaluate(channel, frame):
    # EXE 44b740, encoding 3: binary search upper + float32(1e-6),
    # then ((globalFrame*C)+B)*globalFrame+A. Global, not segment-local time.
    keys = channel['keys']
    index = min(len(keys)-1, bisect.bisect_left([k[0] for k in keys], frame - float(np.float32(1e-6))))
    _, a, b, c = keys[index]
    return np.float32((frame * c + b) * frame + a)


def quaternion_matrix(q, translation):
    x,y,z,w = (float(v) for v in q)
    # D3DXMatrixRotationQuaternion does not normalize the source polynomial output.
    return np.array([[1-2*(y*y+z*z),2*(x*y+z*w),2*(x*z-y*w),0],
                     [2*(x*y-z*w),1-2*(x*x+z*z),2*(y*z+x*w),0],
                     [2*(x*z+y*w),2*(y*z-x*w),1-2*(x*x+y*y),0],
                     [*translation,1]],dtype='<f4')


def wkmd(data):
    if data[:8] != b'WKMD0010' or struct.unpack_from('<I',data,8)[0] != len(data):
        raise ValueError('unit WKMD header')
    binds, bp, nodes, hp, variants, streams, opaque, alpha = struct.unpack_from('<8I',data,48)
    if not 1 <= nodes <= 64 or not nodes <= binds <= 128 or variants != 1 or streams != 1 or not 1 <= opaque + alpha <= 32:
        raise ValueError('unit WKMD rig/draw counts')
    if (bp + binds * 68 + 15) // 16 * 16 != hp or hp + nodes * 40 > len(data):
        raise ValueError('unit WKMD hierarchy offsets')
    matrices = np.frombuffer(data,'<f4',binds*16,bp).reshape(binds,4,4).copy()
    maps = list(struct.iter_unpack('<2H',data[bp+binds*64:bp+binds*68]))
    hierarchy = []
    for i in range(nodes):
        node,parent,*values = struct.unpack_from('<HH9f',data,hp+i*40)
        if node != i or (parent != 65535 and parent >= i) or not all(math.isfinite(v) for v in values) or values[:3] != [1,1,1]:
            raise ValueError('unit WKMD hierarchy topology/scale')
        hierarchy.append(dict(node=node,parent=-1 if parent==65535 else parent,scale=values[:3],euler=values[3:6],translation=values[6:9]))
    if not np.isfinite(matrices).all() or np.max(np.abs(matrices[:,:,3] - [0,0,0,1])) > 1e-6:
        raise ValueError('unit WKMD non-affine bind matrix')
    code,stride,nv,vp,cp,_,_,_,indexcode,ni,ip = struct.unpack_from('<11I',data,80)
    explicit = {0x112:0,0x116:1,0x11a:3}.get(code)
    if explicit is None or stride != (8+explicit)*4 or indexcode != 0x65 or not 1 <= nv <= 30000 or not 3 <= ni <= 100000:
        raise ValueError('unit WKMD stream layout')
    if vp+nv*stride>len(data) or cp+nv*4>len(data) or ip+ni*2>len(data):
        raise ValueError('unit WKMD stream bounds')
    vertices=np.frombuffer(data,'<f4',nv*(8+explicit),vp).reshape(nv,8+explicit)
    colors=np.frombuffer(data,np.uint8,nv*4,cp).reshape(nv,4)
    strip=np.frombuffer(data,'<u2',ni,ip)
    if not np.isfinite(vertices).all() or int(strip.max())>=nv:
        raise ValueError('unit WKMD vertex/index contract')
    draws,attributes,bones,weights,indices = [],[],[],[],[]
    palette_nodes=[-1]*binds
    for i,(lo,hi) in enumerate(maps):
        if hi < nodes:
            if lo!=0:raise ValueError('unit external-rig mapping')
            palette_nodes[i]=hi
    # Slot zero is the literal identity in 44aff0, independent of skeleton root.
    matrices[0]=np.eye(4,dtype='<f4'); palette_nodes[0]=-1
    for d in range(opaque+alpha):
        dp=struct.unpack_from('<I',data,160+d*4)[0]
        if dp+44>len(data):raise ValueError('unit draw bounds')
        flags,texture,unused,*rest=struct.unpack_from('<IH5H7I',data,dp)
        palette=rest[:4];stream,state,primitive,first,length,firstindex,primitives=rest[4:]
        skin=(flags>>16)&255
        if texture!=65535 or unused!=0 or stream!=0 or state!=1 or primitive!=5 or first+length>nv or firstindex+primitives+2>ni:
            raise ValueError('unit draw contract')
        if explicit and skin!=explicit+1 or not explicit and skin:
            raise ValueError('unit draw FVF palette mismatch')
        base=sum(len(a) for a in attributes)
        v=vertices[first:first+length]
        c=colors[first:first+length][:,[2,1,0,3]].astype('<f4')/255
        attributes.append(np.concatenate([v[:,:3],v[:,3+explicit:6+explicit],v[:,6+explicit:8+explicit],c],axis=1).astype('<f4'))
        w=np.zeros((length,4),dtype='<f4');b=np.zeros((length,4),dtype='<u2')
        if explicit:
            w[:,:explicit]=v[:,3:3+explicit]
            w[:,explicit]=1-np.sum(w[:,:explicit],axis=1,dtype=np.float32)
            if np.min(w)<-2e-6 or np.max(w)>1.000002:raise ValueError('unit invalid skin weights')
            for j in range(explicit+1):
                slot=palette[j]
                if slot>=binds or slot!=0 and palette_nodes[slot]<0:raise ValueError('unit unresolved palette')
                b[:,j]=slot
        else:
            node=flags&65535
            if node>=nodes:raise ValueError('unit rigid node')
            # Rigid D3D draws use global bone directly, without an inverse bind.
            slot=len(palette_nodes);palette_nodes.append(node)
            matrices=np.concatenate([matrices,np.eye(4,dtype='<f4')[None]])
            b[:,0]=slot;w[:,0]=1
        bones.append(b);weights.append(w)
        s=strip[firstindex:firstindex+primitives+2].astype(np.int32)-first
        if s.min()<0 or s.max()>=length:raise ValueError('unit draw local index range')
        tri=[]
        for j in range(len(s)-2):
            t=s[j:j+3].tolist()
            if len(set(t))<3:continue
            if j&1:t[0],t[1]=t[1],t[0]
            tri.extend([k+base for k in t])
        indices.extend(tri)
        draws.append(dict(flags=hex(flags),alpha_pass=d>=opaque,source_offset=dp,first=first,vertices=length,source_index=firstindex,source_primitives=primitives,triangles=len(tri)//3,palette=palette))
    return dict(nodes=nodes,binds=binds,hierarchy=hierarchy,matrices=matrices,palette_nodes=palette_nodes,
                attributes=np.concatenate(attributes),bones=np.concatenate(bones),weights=np.concatenate(weights),
                indices=np.array(indices,dtype='<u4'),draws=draws,source_matrices=np.frombuffer(data,'<f4',binds*16,bp).reshape(binds,4,4).copy())


def pose(model,clip,frame):
    if model['nodes']!=clip['nodes']:raise ValueError('unit animation rig mismatch')
    frame=max(0,min(clip['frames']-1,float(frame)))
    translations=np.empty((model['nodes'],3),dtype='<f4');quaternions=np.empty((model['nodes'],4),dtype='<f4')
    for channel in clip['channels']:
        target=translations if channel['kind']<10 else quaternions
        slot=channel['kind']-6 if channel['kind']<10 else channel['kind']-34
        target[channel['node'],slot]=evaluate(channel,frame)
    global_matrices=[]
    for node in model['hierarchy']:
        i=node['node'];m=quaternion_matrix(quaternions[i],translations[i])
        if node['parent']>=0:m=(m@global_matrices[node['parent']]).astype('<f4')
        global_matrices.append(m)
    matrices=np.array([np.eye(4,dtype='<f4') if node<0 else bind@global_matrices[node] for bind,node in zip(model['matrices'],model['palette_nodes'])],dtype='<f4')
    a=model['attributes'];p=np.column_stack([a[:,:3],np.ones(len(a),dtype='<f4')]);n=np.column_stack([a[:,3:6],np.zeros(len(a),dtype='<f4')]);pos=np.zeros((len(a),4),dtype='<f4');norm=pos.copy()
    for k in range(4):
        m=matrices[model['bones'][:,k]];w=model['weights'][:,k,None]
        pos+=np.einsum('vi,vij->vj',p,m)*w;norm+=np.einsum('vi,vij->vj',n,m)*w
    if not np.isfinite(pos).all() or not np.isfinite(norm).all():raise ValueError('unit pose nonfinite')
    return pos[:,:3],norm[:,:3]
