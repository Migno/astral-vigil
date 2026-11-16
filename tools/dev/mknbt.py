import gzip, struct, sys
def tag_str(s): b=s.encode(); return struct.pack('>H',len(b))+b
def named(t,name,payload): return bytes([t])+tag_str(name)+payload
def p_int(v): return struct.pack('>i',v)
def p_list(t,items): return bytes([t])+struct.pack('>i',len(items))+b''.join(items)
def p_compound(entries): return b''.join(entries)+b'\x00'
def c_int(n,v): return named(3,n,p_int(v))
def c_str(n,v): return named(8,n,tag_str(v))
def c_list(n,t,items): return named(9,n,p_list(t,items))
def c_comp(n,entries): return named(10,n,p_compound(entries))
def int_list(vals): return [p_int(v) for v in vals]
sx,sy,sz=int(sys.argv[2]),int(sys.argv[3]),int(sys.argv[4])
floor=sys.argv[5]
layers=int(sys.argv[6]) if len(sys.argv)>6 else 1
palette=[p_compound([c_str('Name',floor)]), p_compound([c_str('Name','minecraft:air')])]
blocks=[]
for x in range(sx):
  for y in range(sy):
    for z in range(sz):
      blocks.append(p_compound([c_list("pos",3,int_list([x,y,z])), c_int("state",0 if y<layers else 1)]))
root=named(10,'',p_compound([
  c_int('DataVersion',3955),
  c_list('size',3,int_list([sx,sy,sz])),
  c_list('palette',10,palette),
  c_list('blocks',10,blocks),
  c_list('entities',10,[]),
]))
open(sys.argv[1],'wb').write(gzip.compress(root))
