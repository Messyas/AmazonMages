from PIL import Image, ImageDraw
from pathlib import Path
import random

OUT = Path('app/src/main/res/drawable-nodpi')
OUT.mkdir(parents=True, exist_ok=True)

def rgba(hexv, a=255):
    hexv=hexv.lstrip('#'); return tuple(int(hexv[i:i+2],16) for i in (0,2,4))+(a,)

def sheet(name, count, draw_cell, cell=32):
    im=Image.new('RGBA',(count*cell,cell),(0,0,0,0))
    for n in range(count): draw_cell(ImageDraw.Draw(im),(n*cell,0),n,cell)
    im.resize((im.width*2,im.height*2),Image.Resampling.NEAREST).save(OUT/(name+'.png'))

GRASS=[rgba(c) for c in ('#2C8A3A','#379C3D','#32943B','#3A9E43')]
G_DARK=rgba('#1D5A2C'); G_MID=rgba('#46B144'); G_LIGHT=rgba('#74D25A'); G_PALE=rgba('#B4EC78')
DIRT=[rgba(c) for c in ('#5A3A24','#86593A','#B38455','#D6AF78','#EED7A4')]
ROCK=[rgba(c) for c in ('#49382D','#725542','#9A7450','#C49A65')]
WATER=[rgba(c) for c in ('#1A3E7A','#2861B0','#3E8FD6','#7CC4F0','#D2F2FF')]
INK=rgba('#153F2A')

def tuft(d,x,y,k=0):
    col=[G_DARK,G_MID,G_LIGHT,G_PALE]
    d.line((x,y+3,x-2,y-1),fill=col[1+k%2],width=1)
    d.line((x,y+3,x+1,y-2),fill=col[2],width=1)
    if k%3==0:d.point((x-3,y+1),fill=col[1])

def grass_cell(d,o,n,s):
    ox,oy=o; base=GRASS[n]
    d.rectangle((ox,oy,ox+s-1,oy+s-1),fill=base)
    # Quiet base with compact organic tufts; leave most of the tile open.
    rng=random.Random(130+n*71)
    for i in range(16):
        x=ox+rng.randrange(2,30); y=oy+rng.randrange(3,30)
        if i%4==0: d.point((x,y),fill=G_LIGHT)
        else: tuft(d,x,y,(i+n)%4)
    for x,y in ((5,7),(24,23),(15,15)):
        if (x+y+n)%3==0: d.point((ox+x,oy+y),fill=G_PALE)

def grass_overlay(d,o,n,s):
    ox,oy=o
    if n==0: # clover patch
        x,y=ox+15,oy+17
        for dx,dy in ((-2,0),(2,0),(0,-2),(0,2)):
            d.rectangle((x+dx, y+dy, x+dx+1,y+dy+1),fill=G_LIGHT)
        d.point((x,y),fill=G_DARK)
    elif n==1: # tiny yellow and white flowers
        for x,y,c in ((9,12,rgba('#F4D86B')),(23,21,rgba('#F4F1D2'))):
            d.point((ox+x,oy+y),fill=G_DARK); d.point((ox+x,oy+y-1),fill=c)
            d.point((ox+x-1,oy+y),fill=c); d.point((ox+x+1,oy+y),fill=c)
    elif n==2: # pebbles
        for x,y,c in ((11,17,ROCK[2]),(13,18,ROCK[1]),(23,9,ROCK[3])):
            d.point((ox+x,oy+y),fill=c)
    else: # fallen leaves
        for x,y,flip in ((12,13,1),(21,22,-1)):
            d.line((ox+x,oy+y,ox+x+3*flip,oy+y-2),fill=rgba('#1E6436'),width=1)
            d.point((ox+x+2*flip,oy+y-1),fill=G_LIGHT)

sheet('tile_grass',8,lambda d,o,n,s: grass_cell(d,o,n,s) if n<4 else grass_overlay(d,o,n-4,s))

def tall_grass(d,o,n,s):
    ox,oy=o
    if n<2: phase=n
    else: phase=n-2
    # Transparent wind-bent blades with dark bases and distinct highlights.
    for i in range(13):
        x=ox+3+(i*7)%26; base=oy+31-(i%3)*2
        lean=(-1,0,1,-1,1)[(i+phase)%5]
        tip=x+lean*(1+(phase%2)); height=9+(i*5)%10
        mid=base-height//2; top=base-height
        d.line((x,base, x,mid,tip,top),fill=G_DARK if i%3==0 else G_MID,width=1)
        d.line((x,base-2,tip,top+2),fill=G_LIGHT,width=1)
    for i in range(3):
        x=ox+5+i*10; d.point((x,oy+28),fill=G_PALE if (i+n)%2==0 else G_LIGHT)
sheet('tile_tall_grass',5,tall_grass)

def path_cell(d,o,n,s):
    ox,oy=o
    d.rectangle((ox,oy,ox+31,oy+31),fill=GRASS[0])
    rng=random.Random(811+n)
    def edge(horizontal, fixed, seed):
        points=[]
        for i in range(9):
            t=i*4
            jitter=random.Random(seed*29+i*13).randint(-2,2)
            points.append((t, max(0,min(31,fixed+jitter))))
        return points
    if n==0:
        d.rectangle((ox,oy,ox+31,oy+31),fill=DIRT[1])
    elif n==1: # north
        e=edge(True,13,n); d.polygon([(ox,oy),(ox+31,oy)]+[(ox+x,oy+y) for x,y in reversed(e)],fill=DIRT[1])
    elif n==2: # east
        e=edge(False,17,n); d.polygon([(ox+31,oy),(ox+31,oy+31)]+[(ox+x,oy+y) for y,x in reversed(e)],fill=DIRT[1])
    elif n==3: # south
        e=edge(True,18,n); d.polygon([(ox,oy+31),(ox+31,oy+31)]+[(ox+x,oy+y) for x,y in e],fill=DIRT[1])
    elif n==4: # west
        e=edge(False,14,n); d.polygon([(ox,oy),(ox,oy+31)]+[(ox+x,oy+y) for y,x in e],fill=DIRT[1])
    elif n<=8: # curved outer corners
        corner=n-5
        pts=[[(0,0),(31,0),(31,9),(26,11),(22,14),(17,16),(13,21),(10,26),(7,31),(0,31)],
             [(0,0),(31,0),(31,31),(21,31),(19,25),(16,21),(12,18),(8,15),(4,12),(0,10)],
             [(0,0),(10,0),(13,6),(16,11),(20,15),(24,18),(31,21),(31,31),(0,31)],
             [(0,0),(31,0),(31,31),(0,31),(0,22),(6,19),(10,16),(14,12),(17,8),(20,3)]]
        d.polygon([(ox+x,oy+y) for x,y in pts[corner]],fill=DIRT[1])
    else: # inner corners: a grassy pocket cuts into the path
        corner=n-9
        pockets=[[(0,0),(12,0),(13,4),(17,7),(18,12),(13,13),(10,17),(5,17),(4,12),(0,11)],
                 [(20,0),(31,0),(31,11),(27,12),(26,17),(21,16),(19,12),(14,10),(15,6),(19,4)],
                 [(19,18),(24,15),(29,17),(31,21),(31,31),(19,31),(18,26),(15,22)],
                 [(0,19),(5,18),(10,15),(14,19),(13,24),(10,27),(11,31),(0,31)]]
        d.rectangle((ox,oy,ox+31,oy+31),fill=DIRT[1])
        d.polygon([(ox+x,oy+y) for x,y in pockets[corner]],fill=GRASS[0])
    # Small breaks and tufts keep the boundary from reading as a hard rectangle.
    for i in range(11):
        x=ox+rng.randrange(32); y=oy+rng.randrange(32)
        if i%4==0:d.point((x,y),fill=G_MID)
        elif i%3==0:d.point((x,y),fill=DIRT[2])
    for x,y in ((8,19),(25,9)):
        if (x+y+n)%3==0: d.point((ox+x,oy+y),fill=ROCK[2])
sheet('tile_path',13,path_cell)

def water_cell(d,o,n,s):
    ox,oy=o
    d.rectangle((ox,oy,ox+31,oy+31),fill=WATER[1])
    rng=random.Random(n*97+31)
    for i in range(18):
        x=ox+rng.randrange(32); y=oy+rng.randrange(32)
        d.point((x,y),fill=WATER[2] if i%3 else WATER[0])
    # Four animated center variants.
    if n<4:
        for k in range(4):
            x=ox+5+((k*7+n*3)%22); y=oy+7+((k*9+n*5)%19)
            d.line((x,y,x+2+(n%2),y),fill=WATER[3],width=1)
            if (k+n)%2==0:d.point((x+1,y-1),fill=WATER[4])
    else:
        idx=n-4
        # 4 cardinal shores, 4 outer corners, 4 inset corners and a quiet center.
        if idx<4:
            if idx==0:d.rectangle((ox,oy,ox+31,oy+5),fill=DIRT[1])
            elif idx==1:d.rectangle((ox,oy+26,ox+31,oy+31),fill=DIRT[1])
            elif idx==2:d.rectangle((ox,oy,ox+5,oy+31),fill=DIRT[1])
            else:d.rectangle((ox+26,oy,ox+31,oy+31),fill=DIRT[1])
        elif idx<8:
            corner=[(0,0),(26,0),(26,26),(0,26)][idx-4]
            d.rectangle((ox+corner[0],oy+corner[1],ox+corner[0]+5,oy+corner[1]+5),fill=DIRT[1])
        elif idx<12:
            corner=[(0,0),(26,0),(26,26),(0,26)][idx-8]
            for j in range(7):
                if j%2==0:d.point((ox+corner[0]+(j%3),oy+corner[1]+(j//3)),fill=DIRT[2])
        else:
            for x in range(7,26,6):d.line((ox+x,oy+14,ox+x+3,oy+14),fill=WATER[3],width=1)
sheet('tile_water',17,water_cell)

def ledge(d,o,n,s):
    ox,oy=o
    d.rectangle((ox,oy,ox+31,oy+31),fill=DIRT[1])
    d.rectangle((ox,oy+10,ox+31,oy+31),fill=ROCK[1])
    for y in (16,23,29):
        for x in range(0,32,7):
            if (x+y+n)%3: d.line((ox+x,oy+y,ox+x+3,oy+y),fill=ROCK[2] if y<23 else ROCK[0])
    # Grass cap, with end shapes and two center variations.
    start=0 if n!=0 else 7; end=31 if n!=3 else 24
    d.rectangle((ox+start,oy+3,ox+end,oy+9),fill=GRASS[1])
    for x in range(start,end+1,4):
        yy=oy+2+((x+n)%3==0)
        d.point((ox+x,yy),fill=G_LIGHT)
        d.point((ox+x,oy+10),fill=G_DARK if x%2 else G_MID)
    for x in range(start+3,end,8): d.line((ox+x,oy+8,ox+x+1,oy+12),fill=G_MID,width=1)
sheet('tile_ledge',4,ledge)

def cliff_cell(d,o,n,s):
    ox,oy=o
    d.rectangle((ox,oy,ox+31,oy+31),fill=ROCK[1])
    rng=random.Random(n+991)
    # Thirteen reusable wall faces with irregular edge lips in the same order as path.
    if n in (1,5,6,9,10): d.rectangle((ox,oy,ox+31,oy+4),fill=GRASS[1])
    if n in (3,7,8,11,12): d.rectangle((ox,oy+27,ox+31,oy+31),fill=ROCK[0])
    for k in range(5):
        y=oy+6+k*5+(k+n)%2
        x=ox+rng.randrange(0,8)
        d.line((x,y,x+rng.randrange(9,25),y),fill=ROCK[2] if k%2 else ROCK[0],width=1)
    for k in range(4):
        x=ox+rng.randrange(3,29); y=oy+rng.randrange(2,30)
        d.point((x,y),fill=G_MID if k%2 else G_LIGHT)
sheet('tile_cliff',13,cliff_cell)

def ground(d,o,n,s):
    ox,oy=o; colors=(DIRT[1],ROCK[1],DIRT[2])
    d.rectangle((ox,oy,ox+31,oy+31),fill=colors[n])
    rng=random.Random(137+n*7)
    for i in range(24):
        x=ox+rng.randrange(32); y=oy+rng.randrange(32)
        d.point((x,y),fill=(DIRT[2] if i%4==0 else DIRT[0]) if n!=1 else ROCK[2 if i%3==0 else 0])
    if n==1:
        for x,y in ((7,8),(23,24)):d.point((ox+x,oy+y),fill=G_MID)
sheet('tile_ground',3,ground)

def lava(d,o,n,s):
    ox,oy=o
    d.rectangle((ox,oy,ox+31,oy+31),fill=rgba('#51251E'))
    rng=random.Random(43+n*23)
    for i in range(20):
        x=ox+rng.randrange(32); y=oy+rng.randrange(32)
        d.point((x,y),fill=rgba('#7A3322') if i%2 else rgba('#9C4A25'))
    paths=[[(4,8),(10,11),(15,9),(20,14),(28,11)],[(4,20),(9,17),(14,21),(21,18),(28,22)],[(6,5),(10,10),(8,15),(14,20),(12,27)],[(25,4),(20,9),(23,14),(18,20),(22,27)]]
    pts=[(ox+x,oy+y) for x,y in paths[n]]
    d.line(pts,fill=rgba('#E08C35'),width=2)
    d.line(pts[1:-1],fill=rgba('#F6C65C'),width=1)
    d.point((ox+pts[2][0]-ox,oy+pts[2][1]-oy),fill=rgba('#FFF0A0'))
sheet('tile_lava',4,lava)

def acid(d,o,n,s):
    ox,oy=o; d.rectangle((ox,oy,ox+31,oy+31),fill=rgba('#233D2C'))
    rng=random.Random(201+n)
    # Pools are irregular, bubbling and lime/purple rather than Leaf emerald/gold.
    for i in range(4):
        x=ox+5+rng.randrange(22); y=oy+7+rng.randrange(18); r=4+(i+n)%4
        col=rgba('#4E9B37') if i%2 else rgba('#7EAF36')
        d.ellipse((x-r,y-r//2,x+r,y+r//2),fill=col)
        d.point((x-1,y-1),fill=rgba('#B6ED69'))
    for x,y in ((8,8),(22,24),(26,12)):
        if (x+y+n)%2:d.point((ox+x,oy+y),fill=rgba('#A35DD0'))
sheet('tile_acid_swamp',4,acid)
