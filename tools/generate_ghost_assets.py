from PIL import Image, ImageDraw
from pathlib import Path
import math

OUT=Path('app/src/main/res/drawable-nodpi'); OUT.mkdir(parents=True,exist_ok=True)
DIRECTIONS=((0,1),(1,1),(1,0),(1,-1),(0,-1),(-1,-1),(-1,0),(-1,1))
ANIMATIONS={
    'idle':(4,12,True),'move':(6,7,True),'alert':(4,9,False),
    'spotted_player':(4,9,False),'startled':(4,8,False),'spotted_ally':(4,10,False),
    'social':(4,10,True),'flee':(6,6,True),'chase':(6,7,True),
    'attack':(6,7,False),'hurt':(4,7,False),'enraged':(4,8,True),
    'fusion':(8,8,False),'death':(8,8,False),
}
P={
    'outline':'#35452C','shade':'#587231','deep':'#79A72C','base':'#A8D92D',
    'light':'#CDF354','shine':'#F2FF9B','eye':'#F3F5D5','iris':'#9B4BCE',
    'pupil':'#512A7D','bubble':'#E6FB78','acid':'#C0EA30','dark':'#6B39A5',
}

def transparent(w,h): return Image.new('RGBA',(w,h),(0,0,0,0))

def contour(cx,top,w,h,phase=0,spiky=False):
    pts=[]
    for i in range(32):
        a=2*math.pi*i/32-math.pi/2
        ripple=math.sin(i*2.1+phase)*.7+math.sin(i*.73+phase)*.45
        if spiky: ripple+=1.6*math.cos(i*math.pi/2+phase)
        rx=w/2+ripple
        x=cx+math.cos(a)*rx
        y=top+h*(.5+.5*math.sin(a))
        # A tapered, undulating lower edge keeps the ghost from reading as a ball.
        if math.sin(a)>.42: y+=math.sin(a)*1.25+math.sin(i*1.8+phase)*.7
        pts.append((round(x),round(y)))
    return pts

def draw_eyes(d,cx,cy,dx,dy,scale,blink=False,eyes=2):
    gap=max(4,round(scale*.25)); size=max(2,round(scale*.13))
    shiftX=round(dx*scale*.12); shiftY=round(-dy*scale*.08)
    if dy<-.35:  # facing north: show the back of the translucent head
        d.ellipse((cx-gap,cy,cx-gap+size,cy+size),fill=P['shade'])
        d.ellipse((cx+gap-size,cy+1,cx+gap,cy+1+size),fill=P['shade'])
        return
    if blink:
        d.line((cx-gap-size+shiftX,cy+shiftY,cx-gap+shiftX,cy+shiftY),fill=P['outline'],width=1)
        if eyes>1: d.line((cx+gap-size+shiftX,cy+shiftY,cx+gap+shiftX,cy+shiftY),fill=P['outline'],width=1)
        return
    if eyes==1:
        positions=[cx+shiftX]
    elif eyes==2:
        positions=[cx-gap+shiftX,cx+gap+shiftX]
    else:
        positions=[cx-2*gap+shiftX,cx-gap//2+shiftX,cx+gap//2+shiftX,cx+2*gap+shiftX]
    for j,ex in enumerate(positions):
        ew=size if eyes==1 else max(2,size-1)
        ey=cy+shiftY
        d.ellipse((ex-ew,ey-1,ex+ew,ey+size+1),fill=P['outline'])
        d.rectangle((ex-ew+1,ey,ex+ew-1,ey+size),fill=P['eye'])
        pupilX=ex+max(-1,min(1,round(dx*.7)))
        pupilY=ey+max(0,min(size-1,round(-dy*.35)))
        d.point((pupilX,pupilY),fill=P['iris'])
        if ew>2: d.point((pupilX,pupilY+1),fill=P['pupil'])
        d.point((ex-ew+1,ey),fill=P['shine'])

def draw_ghost(action,frame,direction,fused=False):
    logical=48 if fused else 32
    scale=logical/32
    dX,dY=DIRECTIONS[direction]
    tick=frame/(ANIMATIONS[action][0]-1 or 1)
    cell=transparent(logical,logical); d=ImageDraw.Draw(cell)
    cx=logical//2; cy=logical//2
    sizeX=(24 if fused else 18)*scale
    sizeY=(25 if fused else 21)*scale
    bob=0
    squash=1.0
    phase=frame*1.1+direction*.6
    offset=0
    spikes=action=='enraged'
    blink=action=='idle' and frame==3
    if action in ('move','chase','flee','social'):
        bob=round(math.sin(frame*math.pi/2)*scale)
        squash=1+(.07 if frame%3==0 else -.04 if frame%3==1 else 0)
        offset=round(math.sin(frame*math.pi/3)*scale)
    elif action in ('alert','spotted_player','spotted_ally','startled'):
        sizeY*=1.06
        bob=-round((1 if frame%2==0 else 0)*scale)
        offset=round(dX*scale*(1 if frame>0 else 0))
    elif action=='attack':
        reach=tick if frame<3 else max(0,1-(frame-3)/3)
        offset=round(dX*reach*4*scale)
        sizeX*=1+reach*.18; sizeY*=1-reach*.06
    elif action=='hurt':
        offset=round((-dX if frame%2==0 else dX)*2*scale)
        squash=1-(.12 if frame==1 else 0)
    elif action=='fusion':
        sizeX*=.65+tick*.4; sizeY*=.72+tick*.32
        bob=-round(math.sin(tick*math.pi)*2*scale)
    elif action=='death':
        squash=max(.06,1-frame/(ANIMATIONS[action][0]-1))
        sizeY*=squash
        bob=round(frame*1.4*scale)
    elif action=='enraged':
        sizeX*=1.06; sizeY*=1.04; bob=round(math.sin(frame*math.pi/2)*scale)

    if fused:
        sizeX*=1.36; sizeY*=1.22
    sizeX=max(3,sizeX); sizeY=max(2,sizeY)
    cx+=offset+round(dX*scale*.6)
    cy+=bob-round(dY*scale*.5)
    top=round(cy-sizeY*.5)
    phase+=frame*.35
    body=contour(cx,top,sizeX,sizeY,phase,spikes)
    d.polygon(body,fill=P['outline'])
    inset=1 if not fused else 2
    inner=contour(cx,top+inset,sizeX-inset*2,sizeY-inset*2,phase,spikes)
    d.polygon(inner,fill=P['deep'])
    # Discrete upper-left light and lower-right acid shadow.
    d.polygon([(cx-sizeX*.34,top+sizeY*.28),(cx-sizeX*.19,top+sizeY*.14),
        (cx+sizeX*.04,top+sizeY*.15),(cx-sizeX*.03,top+sizeY*.30),
        (cx-sizeX*.22,top+sizeY*.36)],fill=P['light'])
    d.polygon([(cx-sizeX*.14,top+sizeY*.27),(cx-sizeX*.03,top+sizeY*.21),
        (cx+sizeX*.09,top+sizeY*.25)],fill=P['shine'])
    d.polygon([(cx+sizeX*.16,top+sizeY*.68),(cx+sizeX*.37,top+sizeY*.58),
        (cx+sizeX*.27,top+sizeY*.82),(cx+sizeX*.07,top+sizeY*.88)],fill=P['shade'])

    if action=='death' and frame>=6:
        # The ghost collapses into a small acid puddle, keeping only opaque pixels.
        d.rectangle((0,0,logical,logical),fill=(0,0,0,0))
        puddleW=max(4,round((logical*.34)*(1-(frame-6)*.32)))
        d.ellipse((cx-puddleW,cy+2,cx+puddleW,cy+5),fill=P['shade'],outline=P['outline'])
        if frame==6: d.point((cx-2,cy+2),fill=P['iris'])
    else:
        eyeY=round(top+sizeY*.43)
        if action in ('attack','enraged'): eyeY-=round(scale)
        eyes=1 if direction in (2,6) else 2
        draw_eyes(d,cx,eyeY,dX,dY,scale,blink,eyes if not fused else eyes*2)
        # A couple of unmistakable acid bubbles and directional motion accents.
        if action in ('move','chase','flee','social','attack','fusion') and frame%2==0:
            bx=cx-round(dX*sizeX*.42); by=cy+round(sizeY*.32)
            d.rectangle((bx-1,by-1,bx+1,by+1),fill=P['bubble'])
            if action in ('chase','attack'):
                d.point((cx+round(dX*sizeX*.57),cy-round(dY*sizeY*.18)),fill=P['acid'])
        if action=='enraged':
            for n in (-1,0,1):
                x=cx+n*round(scale*5); y=top-round(scale*(2+abs(n)))
                d.polygon([(x-1,y+2),(x,y),(x+2,y+2)],fill=P['dark'])
        if action=='fusion':
            for n in range(3):
                x=cx+round(math.sin(frame+n*2)*sizeX*.52)
                y=cy+round(math.cos(frame+n*2)*sizeY*.48)
                d.point((x,y),fill=P['bubble'])
        if action=='attack' and frame>=2:
            reach=sizeX*.55
            for n in range(3):
                x=round(cx+dX*(reach+n*scale*2)); y=round(cy-dY*(reach+n*scale*2))
                d.rectangle((x-1,y-1,x+1,y+1),fill=P['acid'] if n%2 else P['bubble'])

    # Fade-like final frames use smaller opaque shapes, never semitransparent pixels.
    return cell.resize((logical*2,logical*2),Image.Resampling.NEAREST)

def generate(action,frames,fused):
    cell=96 if fused else 64
    sheet=transparent(frames*cell,8*cell)
    for row in range(8):
        for col in range(frames):
            sheet.alpha_composite(draw_ghost(action,col,row,fused),(col*cell,row*cell))
    name='acid_ghost_fused_' if fused else 'acid_ghost_'
    sheet.save(OUT/f'{name}{action}.png',optimize=True)

for action,(frames,_,_) in ANIMATIONS.items():
    generate(action,frames,False)
    generate(action,frames,True)
print(f'Generated {len(ANIMATIONS)*2} directional ghost animation atlases')
