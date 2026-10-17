from PIL import Image, ImageDraw
from pathlib import Path
import random

OUT=Path('app/src/main/res/drawable-nodpi'); OUT.mkdir(parents=True,exist_ok=True)
def C(h,a=255):
    h=h.lstrip('#'); return tuple(int(h[i:i+2],16) for i in (0,2,4))+(a,)
GREEN=[C(x) for x in ('#123F2A','#1E6436','#2F8B3E','#55B24A','#94D768')]
WOOD=[C(x) for x in ('#4A3025','#70442D','#9A623A','#C58B50','#E1B96D')]
FLOWER=[C(x) for x in ('#F4D86B','#F3EEE0','#E66C54','#E88FB1','#B5D94D','#66C9B6')]
INK=C('#102E24'); SHADOW=C('#10281D',120)

def sheet(name,count,w,h,draw,scale=2):
    im=Image.new('RGBA',(count*w,h),(0,0,0,0))
    for n in range(count):draw(ImageDraw.Draw(im),(n*w,0),n,w,h)
    im.resize((im.width*scale,im.height*scale),Image.Resampling.NEAREST).save(OUT/(name+'.png'))

def shadow(d,ox,oy,cx,cy,rx,ry):
    d.ellipse((ox+cx-rx,oy+cy-ry,ox+cx+rx,oy+cy+ry),fill=SHADOW)

def round_tree(d,o,n,w,h):
    ox,oy=o; shadow(d,ox,oy,32,57,24,5)
    # Visible roots and the lower face of the trunk.
    d.polygon([(ox+27,oy+43),(ox+37,oy+43),(ox+40,oy+57),(ox+34,oy+54),(ox+31,oy+59),(ox+28,oy+53),(ox+23,oy+56)],fill=WOOD[0])
    d.rectangle((ox+28,oy+35,ox+36,oy+51),fill=WOOD[2])
    d.line((ox+30,oy+39,ox+30,oy+49),fill=WOOD[3]); d.line((ox+34,oy+37,ox+35,oy+48),fill=WOOD[1])
    rng=random.Random(740+n)
    # Lobe outlines are overlapping, stepped circles with broad light-facing crowns.
    lobes=[(17,24,12),(29,17,14),(43,24,12),(14,34,10),(28,31,15),(43,35,11),(23,42,10),(37,43,9)]
    for x,y,r in lobes:
        x+=rng.randrange(-1,2); y+=rng.randrange(-1,2)
        d.ellipse((ox+x-r-1,oy+y-r-1,ox+x+r+1,oy+y+r+1),fill=INK)
        d.ellipse((ox+x-r,oy+y-r,ox+x+r,oy+y+r),fill=GREEN[1+n%2])
        d.ellipse((ox+x-r+2,oy+y-r+2,ox+x+r-2,oy+y+r-2),fill=GREEN[2+n%2])
        d.arc((ox+x-r+3,oy+y-r+3,ox+x+r-3,oy+y+r-3),190,315,fill=GREEN[4],width=2)
    for i in range(14):
        x=ox+rng.randrange(12,53); y=oy+rng.randrange(13,47)
        d.point((x,y),fill=GREEN[4] if i%3==0 else GREEN[3])
sheet('obj_tree_round',3,64,64,round_tree)

def palm(d,o,n,w,h):
    ox,oy=o; shadow(d,ox,oy,32,91,18,4)
    d.polygon([(ox+29,oy+88),(ox+35,oy+88),(ox+38,oy+31),(ox+31,oy+28)],fill=WOOD[1])
    d.line((ox+33,oy+39,ox+34,oy+82),fill=WOOD[3],width=2)
    for y in range(47,86,8): d.arc((ox+28,oy+y,ox+37,oy+y+7),0,180,fill=WOOD[0],width=1)
    cx,cy=33,29
    # Six arcing fronds, each with a stepped leaflet rhythm.
    for i in range(7):
        ang=(-2.85+i*.47)+(0.08 if n else 0)
        dx,dy=__import__('math').cos(ang),__import__('math').sin(ang)
        ex=cx+dx*28; ey=cy+dy*21
        d.line((ox+cx,oy+cy,ox+round(ex),oy+round(ey)),fill=GREEN[0],width=2)
        for t in range(3,10):
            px=cx+dx*t*2.6; py=cy+dy*t*2.0
            side=(-1 if t%2 else 1)
            lx=round(px-dy*side*4); ly=round(py+dx*side*4)
            d.line((ox+round(px),oy+round(py),ox+lx,oy+ly),fill=GREEN[2+(i+t)%3],width=2 if t%3 else 3)
    # Small açaí/buriti cluster below the crown.
    fruit=C('#692D55') if n==0 else C('#DA8C39')
    for x,y in ((31,35),(35,36),(33,40),(37,41)):
        d.ellipse((ox+x-2,oy+y-2,ox+x+2,oy+y+2),fill=INK)
        d.ellipse((ox+x-1,oy+y-2,ox+x+1,oy+y),fill=fruit)
sheet('obj_tree_palm',2,64,96,palm)

def giant(d,o,n,w,h):
    ox,oy=o; shadow(d,ox,oy,48,87,37,7)
    # Buttress roots spreading across ground.
    d.polygon([(ox+42,oy+63),(ox+54,oy+63),(ox+67,oy+92),(ox+57,oy+89),(ox+49,oy+75),(ox+39,oy+92),(ox+31,oy+88)],fill=WOOD[0])
    d.polygon([(ox+42,oy+25),(ox+56,oy+25),(ox+59,oy+82),(ox+51,oy+77),(ox+46,oy+88),(ox+40,oy+76)],fill=WOOD[1])
    d.line((ox+47,oy+31,ox+49,oy+72),fill=WOOD[3],width=3)
    d.line((ox+54,oy+35,ox+54,oy+69),fill=WOOD[0],width=2)
    rng=random.Random(481+n)
    for x,y,r in ((26,23,18),(46,17,20),(67,24,18),(29,42,18),(50,38,22),(72,42,17),(43,55,18),(62,55,16)):
        d.ellipse((ox+x-r-1,oy+y-r-1,ox+x+r+1,oy+y+r+1),fill=INK)
        d.ellipse((ox+x-r,oy+y-r,ox+x+r,oy+y+r),fill=GREEN[1])
        d.ellipse((ox+x-r+3,oy+y-r+3,ox+x+r-3,oy+y+r-3),fill=GREEN[2+(n%2)])
        d.arc((ox+x-r+4,oy+y-r+3,ox+x+r-4,oy+y+r-4),195,320,fill=GREEN[4],width=2)
    for i in range(24):
        x=ox+rng.randrange(14,84); y=oy+rng.randrange(7,68)
        d.point((x,y),fill=GREEN[4] if i%3==0 else GREEN[3])
sheet('obj_tree_giant',1,96,96,giant)

def bush(d,o,n,w,h):
    ox,oy=o; shadow(d,ox,oy,16,29,13,3)
    shapes=[[(8,20,7),(16,13,8),(24,20,6),(12,24,6),(21,25,6)],
            [(7,17,6),(14,21,7),(22,14,7),(26,22,5),(16,27,6)],
            [(8,23,7),(12,14,6),(20,17,8),(26,23,6),(17,25,7)],
            [(6,20,5),(13,16,8),(21,20,7),(26,16,5),(17,26,6)]][n]
    rng=random.Random(68+n)
    for x,y,r in shapes:
        d.ellipse((ox+x-r-1,oy+y-r-1,ox+x+r+1,oy+y+r+1),fill=INK)
        d.ellipse((ox+x-r,oy+y-r,ox+x+r,oy+y+r),fill=GREEN[1])
        d.ellipse((ox+x-r+1,oy+y-r+1,ox+x+r-1,oy+y+r-1),fill=GREEN[2+n%2])
        d.point((ox+x-2,oy+y-2),fill=GREEN[4])
    for i in range(4):
        x=ox+rng.randrange(5,28); y=oy+rng.randrange(12,26)
        if i%2==n%2:d.point((x,y),fill=GREEN[4])
sheet('obj_bush',4,32,32,bush)

def flowers(d,o,n,w,h):
    ox,oy=o; shadow(d,ox,oy,16,29,13,2)
    # Leafy stems and unique bloom silhouettes for six clumps.
    configs=[(1,4,5),(2,5,6),(3,6,4),(4,4,5),(5,5,6),(0,6,4)]
    bloom,count,height=configs[n]
    for i in range(count):
        x=ox+5+i*4+(i%2); y=oy+27-(i%3)*2
        top=y-height-(i%2)
        d.line((x,y,x,top),fill=GREEN[1],width=1)
        d.ellipse((x-3,oy+22+(i%3),x+1,oy+24+(i%3)),fill=GREEN[2])
        if n in (2,4):
            d.polygon([(x,top-2),(x+2,top),(x,top+4),(x-2,top)],fill=FLOWER[bloom])
            d.point((x,top),fill=FLOWER[0])
        else:
            col=FLOWER[bloom]
            for dx,dy in ((0,0),(-1,0),(1,0),(0,-1),(0,1)):
                d.point((x+dx,top+dy),fill=col)
            d.point((x,top),fill=FLOWER[0] if bloom else C('#D69C39'))
sheet('obj_flowers',6,32,32,flowers)

def rock(d,o,n,w,h):
    ox,oy=o; shadow(d,ox,oy,16,29,12,3)
    outlines=[[(4,25),(5,17),(10,12),(17,10),(24,15),(28,23),(24,28),(8,28)],
              [(3,26),(7,17),(8,13),(17,11),(22,16),(27,17),(29,25),(20,28)],
              [(4,26),(5,19),(11,15),(12,10),(21,12),(27,19),(26,27),(9,29)],
              [(3,24),(8,16),(15,13),(18,10),(24,15),(29,23),(24,28),(8,27)]]
    pts=[(ox+x,oy+y) for x,y in outlines[n]]
    d.polygon(pts,fill=INK)
    d.polygon([(ox+x+1,oy+y+1) for x,y in outlines[n]],fill=C('#725542'))
    d.polygon([(ox+7,oy+22),(ox+10,oy+16),(ox+16,oy+13),(ox+20,oy+18),(ox+17,oy+23)],fill=C('#B18C60'))
    d.line((ox+8,oy+25,ox+21,oy+25),fill=C('#49382D'),width=1)
    d.point((ox+22,oy+20),fill=GREEN[2]); d.point((ox+23,oy+19),fill=GREEN[3])
sheet('obj_rock',4,32,32,rock)

def sign(d,o,n,w,h):
    ox,oy=o; shadow(d,ox,oy,16,30,12,2)
    d.line((ox+15,oy+12,ox+15,oy+29),fill=WOOD[0],width=4)
    d.line((ox+16,oy+14,ox+16,oy+27),fill=WOOD[2],width=2)
    d.polygon([(ox+3,oy+4),(ox+27,oy+4),(ox+29,oy+6),(ox+27,oy+17),(ox+4,oy+18),(ox+2,oy+15)],fill=WOOD[0])
    d.polygon([(ox+4,oy+5),(ox+26,oy+5),(ox+27,oy+7),(ox+25,oy+15),(ox+5,oy+16)],fill=WOOD[2])
    d.line((ox+8,oy+7,ox+8,oy+13),fill=WOOD[1]); d.line((ox+12,oy+7,ox+18,oy+7),fill=WOOD[4])
    d.point((ox+23,oy+10),fill=GREEN[3])
sheet('obj_sign',1,32,32,sign)

def fence(d,o,n,w,h):
    ox,oy=o
    # Three joinable fence segments: left cap, repeatable middle, right cap.
    d.line((ox+3,oy+13,ox+29,oy+13),fill=WOOD[0],width=5)
    d.line((ox+3,oy+24,ox+29,oy+24),fill=WOOD[0],width=5)
    d.line((ox+4,oy+12,ox+28,oy+12),fill=WOOD[3],width=2)
    d.line((ox+4,oy+23,ox+28,oy+23),fill=WOOD[2],width=2)
    posts=[(3,),(3,28),(28,)]
    for x in posts[n]:
        d.polygon([(ox+x-2,oy+29),(ox+x-2,oy+6),(ox+x,oy+3),(ox+x+2,oy+6),(ox+x+2,oy+29)],fill=WOOD[0])
        d.line((ox+x-1,oy+8,ox+x-1,oy+27),fill=WOOD[3],width=1)
sheet('obj_fence',3,32,32,fence)
