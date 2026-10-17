from PIL import Image, ImageDraw
from math import cos, sin, pi
from pathlib import Path

OUT = Path('app/src/main/res/drawable-nodpi')
OUT.mkdir(parents=True, exist_ok=True)
INK = (12, 54, 37, 255)
SHADOW = (15, 74, 46, 255)
MID = (30, 138, 74, 255)
BRIGHT = (61, 196, 94, 255)
LIGHT = (155, 240, 106, 255)
GOLD = (244, 208, 94, 255)
PALE = (244, 247, 160, 255)

def canvas(size):
    return Image.new('RGBA', size, (0, 0, 0, 0))

def poly(d, pts, fill):
    d.polygon(pts, fill=fill)

def leaf(d, cx, cy, length, width, angle, accent=True):
    # Hand-drawn stepped lanceolate blade, pointed at both ends.
    ux, uy = cos(angle), sin(angle)
    vx, vy = -uy, ux
    def p(a, b): return (round(cx + ux*a + vx*b), round(cy + uy*a + vy*b))
    pts = [p(-length/2,0), p(-length/4,-width*.42), p(0,-width/2),
           p(length*.28,-width*.34), p(length/2,0), p(length*.25,width*.34),
           p(0,width/2), p(-length*.28,width*.36)]
    poly(d, pts, INK)
    inner = [p(-length*.38,0), p(-length*.20,-width*.31), p(0,-width*.39),
             p(length*.34,0), p(0,width*.38), p(-length*.20,width*.28)]
    poly(d, inner, MID)
    poly(d, [p(-length*.29,-width*.16),p(-length*.08,-width*.36),p(length*.20,-width*.20),p(length*.35,-width*.02),p(0,-width*.10)], BRIGHT)
    d.line([p(-length*.34,0), p(length*.36,0)], fill=LIGHT, width=1)
    if accent and length > 11:
        d.line([p(-length*.03,-width*.04), p(length*.14,-width*.22)], fill=PALE, width=1)

def rotated_leaf(size, angle):
    im=canvas((size,size)); leaf(ImageDraw.Draw(im),size//2,size//2,size*.72,size*.42,angle)
    return im

def save_sheet(name, cols, rows, w, h, draw_cell, scale=2):
    logical=canvas((cols*w,rows*h))
    d=ImageDraw.Draw(logical)
    for row in range(rows):
        for col in range(cols):
            cell=canvas((w,h)); draw_cell(ImageDraw.Draw(cell),col,row,w,h)
            logical.alpha_composite(cell,(col*w,row*h))
    logical.resize((cols*w*scale,rows*h*scale),Image.Resampling.NEAREST).save(OUT/(name+'.png'))

def projectile(d, frame, direction, w, h):
    # Direction is a true 8-way orientation; each row has its own flight vector.
    a = direction*pi/4 + frame*pi/3
    cx,cy=w//2,h//2
    vx,vy=cos(direction*pi/4),sin(direction*pi/4)
    # Leaf mass leads; two small trailing leaves reinforce motion.
    leaf(d, cx,cy,17,9,a)
    leaf(d, round(cx-vx*7),round(cy-vy*7),7,4,a+2.5,False)
    leaf(d, round(cx-vx*10),round(cy-vy*10),6,3,a-2.4,False)
    # Gold seed core.
    d.point((cx,cy),fill=GOLD)
    d.point((cx,cy-1),fill=PALE)

def cast_flash(d, f, row, w, h):
    cx=cy= w//2
    radius=(2,4,7,9,11)[f]
    alpha=(80,160,255,160,80)[f]
    pale=(155,240,106,alpha); gold=(244,208,94,alpha)
    n=3+f
    for i in range(n):
        a=2*pi*i/n + f*.24
        x=round(cx+cos(a)*radius); y=round(cy+sin(a)*radius)
        leaf(d,x,y,5,3,a,False)
    d.point((cx,cy),fill=PALE if f<3 else pale)
    if f in (2,3):
        for dx,dy in ((-1,0),(1,0),(0,-1),(0,1)):
            d.point((cx+dx*(f-1),cy+dy*(f-1)),fill=gold)

def impact(d, f, row, w, h):
    cx=cy=16
    ring=(2,5,9,12,14,15)[f]
    # Flattened ground ring, broken into discrete arcs.
    for arc in range(3):
        start=arc*120+f*9
        for deg in range(start,start+70,12):
            a=deg*pi/180
            x=round(cx+cos(a)*ring); y=round(cy+sin(a)*ring*.48)
            d.point((x,y),fill=(61,196,94,(255,160,80,80,80,0)[f]))
    count=(2,4,6,5,3,1)[f]
    for i in range(count):
        a=2*pi*i/max(count,1)+f*.31
        dist=(3,6,9,11,12,13)[f]
        leaf(d,round(cx+cos(a)*dist),round(cy+sin(a)*dist*.72),7,4,a+f*.4)
    if f<4:
        for dx,dy in ((-10,-5),(10,-4),(-6,8),(7,9))[:max(1,4-f//2)]:
            d.point((cx+dx+(f%2),cy+dy),fill=PALE if f<3 else GOLD)

def storm(d, f, row, w, h):
    cx=cy=32
    if f<2:
        r=4+f*5
        for i in range(5+f*2):
            a=2*pi*i/(5+f*2)
            leaf(d,round(cx+cos(a)*r),round(cy+sin(a)*r*.45),10,5,a+pi/2)
    elif f<8:
        phase=f-2
        # Ground shadow and ascending helical bands with gaps for readable motion.
        for x in range(-8,9,2):
            yy=round(cy+12+abs(x)//3)
            d.point((cx+x,yy),fill=(15,74,46,80))
        for band in range(3):
            y=cy+13-band*11
            rx=7+band*4
            for i in range(5):
                a=(i/5)*2*pi+phase*.58+band*1.35
                x=round(cx+cos(a)*rx)
                yy=round(y+sin(a)*4)
                leaf(d,x,yy,12,6,a+pi/2)
        for i in range(3):
            a=phase*.7+i*2.1
            d.point((round(cx+cos(a)*17),round(cy-15+sin(a)*4)),fill=PALE if i==0 else LIGHT)
    else:
        remain=10-f
        for i in range(remain*2):
            a=2*pi*i/max(1,remain*2)+f*.3
            r=8+(i%3)*6
            leaf(d,round(cx+cos(a)*r),round(cy+sin(a)*r*.5),8,4,a+pi/2)

def warning(d,f,row,w,h):
    cx=cy=16; r=(8,10,12,9)[f]
    alpha=(80,160,255,160)[f]
    color=(61,196,94,alpha)
    for i in range(12):
        a=2*pi*i/12
        x=round(cx+cos(a)*r); y=round(cy+sin(a)*r*.5)
        if i%3==f%3:
            leaf(d,x,y,5,3,a,False)
        else:
            d.point((x,y),fill=color)
    for x,y in ((-3,-1),(3,1)):
        d.line((cx+x,cy+y,cx+x*2,cy+y*2),fill=LIGHT,width=1)

def shield(d,f,direction,w,h):
    # Frontal fan; vector points outward from the actor in the atlas direction.
    a=direction*pi/4
    cx=cy=16
    rx,ry=cos(a),sin(a); px,py=-ry,rx
    for i in range(5):
        t=(i-2)*.34
        dist=8+(i%2)*2
        x=round(cx+rx*dist+px*t*10)
        y=round(cy+ry*dist+py*t*10)
        leaf(d,x,y,10,5,a+pi/2+(f%2)*.18)

def parry(d,f,row,w,h):
    cx=cy=16
    scale=(3,6,9,7,4)[f]
    if f in (1,2,3):
        # Four-point, stepped star, emerald edge with pale center.
        pts=[(cx,cy-scale),(cx+1,cy-1),(cx+scale,cy),(cx+1,cy+1),(cx,cy+scale),(cx-1,cy+1),(cx-scale,cy),(cx-1,cy-1)]
        poly(d,pts,BRIGHT); d.point((cx,cy),fill=PALE)
    for i in range(4):
        a=i*pi/2+f*.24
        leaf(d,round(cx+cos(a)*(scale+4)),round(cy+sin(a)*(scale+4)),6,3,a,False)

def posture(d,f,row,w,h):
    cx=cy=16
    radius=(3,6,9,11,13,14)[f]
    for i in range(12):
        a=2*pi*i/12
        if (i+f)%4==0: continue
        x=round(cx+cos(a)*radius); y=round(cy+sin(a)*radius*.7)
        d.line((cx+round(cos(a)*(radius-3)),cy+round(sin(a)*(radius-3)*.7),x,y),fill=GOLD if f<3 else BRIGHT,width=1)
    if f<4:
        for x,y in ((-3,-2),(4,2),(-2,4)):
            d.point((cx+x*(f//2+1),cy+y*(f//2+1)),fill=PALE)

def rooted(d,f,row,w,h):
    cx=cy=8
    r=(2,4,6,6)[f]
    # Root icon spreads down and around a restrained central sprout.
    d.line((cx,cy-5,cx,cy+2),fill=LIGHT,width=2)
    d.line((cx,cy-3,cx-3,cy-5),fill=BRIGHT,width=1)
    d.line((cx,cy-2,cx+3,cy-4),fill=BRIGHT,width=1)
    d.line((cx,cy+1,cx-r,cy+5),fill=SHADOW,width=2)
    d.line((cx,cy+1,cx+r,cy+5),fill=SHADOW,width=2)
    d.line((cx,cy+2,cx,cy+7),fill=SHADOW,width=2)
    d.point((cx,cy-5),fill=PALE)

save_sheet('leaf_projectile',4,8,24,24,projectile)
save_sheet('leaf_cast_flash',5,1,24,24,cast_flash)
save_sheet('leaf_impact',6,1,32,32,impact)
save_sheet('leaf_storm',10,1,64,64,storm)
save_sheet('leaf_storm_warning',4,1,32,32,warning)
save_sheet('leaf_shield',4,8,32,32,shield)
save_sheet('parry_flash',5,1,32,32,parry)
save_sheet('posture_break',6,1,32,32,posture)
save_sheet('status_rooted',4,1,16,16,rooted)
