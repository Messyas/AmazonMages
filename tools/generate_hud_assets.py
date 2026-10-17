from PIL import Image, ImageDraw
from pathlib import Path

OUT = Path('app/src/main/res/drawable-nodpi')
OUT.mkdir(parents=True, exist_ok=True)
DOCS = Path('docs')

def save(name, im):
    im.save(OUT / f'{name}.png')

def canvas(w, h):
    return Image.new('RGBA', (w,h), (0,0,0,0))

def px(d, points, color): d.polygon(points, fill=color)

def icon(d, x, y, kind, s=16):
    # 16px crisp silhouettes, palette kept deliberately small.
    dark='#24482F'; mid='#3C8C43'; light='#A7E36B'; gold='#F1C85C'; cream='#F4E8C2'; wood='#9A6840'
    if kind in ('leaf','blade','attack'):
        px(d,[(x+2,y+11),(x+4,y+5),(x+10,y+2),(x+14,y+3),(x+12,y+9),(x+6,y+14)],mid)
        d.line((x+3,y+13,x+12,y+4),fill=light,width=1)
        d.line((x+6,y+10,x+5,y+6),fill=light,width=1)
    elif kind=='special':
        d.arc((x+2,y+2,x+14,y+14),35,315,fill=light,width=2); d.arc((x+5,y+5,x+11,y+11),210,35,fill=gold,width=2)
        d.point((x+11,y+4),fill=cream)
    elif kind in ('swap','arrows'):
        d.line((x+3,y+5,x+12,y+5),fill=light,width=2); px(d,[(x+12,y+2),(x+15,y+5),(x+12,y+8)],gold)
        d.line((x+13,y+11,x+4,y+11),fill=mid,width=2); px(d,[(x+4,y+8),(x+1,y+11),(x+4,y+14)],cream)
    elif kind=='shield':
        px(d,[(x+3,y+2),(x+13,y+3),(x+12,y+9),(x+8,y+14),(x+4,y+10)],mid); d.line((x+5,y+4,x+11,y+5),fill=light,width=1)
    elif kind=='potion':
        d.rectangle((x+6,y+2,x+9,y+4),fill=gold); d.rectangle((x+5,y+5,x+10,y+13),fill=mid); d.rectangle((x+6,y+8,x+9,y+11),fill=light)
    elif kind=='talk':
        d.rounded_rectangle((x+2,y+3,x+13,y+11),radius=2,fill=cream); px(d,[(x+5,y+10),(x+4,y+14),(x+9,y+10)],gold)
        d.point((x+5,y+7),fill=wood); d.point((x+8,y+7),fill=wood); d.point((x+11,y+7),fill=wood)
    elif kind=='attributes':
        d.ellipse((x+2,y+2,x+7,y+7),fill=gold); d.ellipse((x+9,y+2,x+14,y+7),fill=light); d.rectangle((x+3,y+9,x+7,y+14),fill=mid); d.rectangle((x+9,y+9,x+13,y+14),fill=wood)
    elif kind=='bag':
        d.rounded_rectangle((x+3,y+5,x+13,y+14),radius=2,fill=wood); d.arc((x+5,y+1,x+11,y+8),180,360,fill=gold,width=2); d.rectangle((x+7,y+8,x+9,y+10),fill=light)
    elif kind=='map':
        px(d,[(x+2,y+4),(x+6,y+2),(x+10,y+4),(x+14,y+2),(x+14,y+12),(x+10,y+14),(x+6,y+12),(x+2,y+14)],cream)
        d.line((x+6,y+3,x+6,y+12),fill=mid); d.line((x+10,y+4,x+10,y+13),fill=mid); d.point((x+8,y+7),fill='#B64B42')
    elif kind=='quest':
        d.rectangle((x+3,y+2,x+12,y+14),fill=cream); d.line((x+5,y+5,x+10,y+5),fill=wood); d.line((x+5,y+8,x+10,y+8),fill=wood); d.line((x+5,y+11,x+9,y+11),fill=mid)
    elif kind=='menu':
        for yy in (4,7,10,13): d.rectangle((x+3,yy,x+12,yy+1),fill=cream)
    elif kind=='close':
        d.line((x+4,y+4,x+12,y+12),fill=cream,width=2); d.line((x+12,y+4,x+4,y+12),fill=gold,width=2)
    elif kind=='coconut':
        d.ellipse((x+3,y+4,x+12,y+13),fill='#8C5434',outline='#422B25'); d.ellipse((x+5,y+5,x+8,y+8),fill='#C98E4E'); d.line((x+9,y+2,x+13,y+1),fill=mid,width=2)
    elif kind=='staff':
        d.line((x+4,y+14,x+11,y+3),fill=wood,width=3); d.ellipse((x+9,y+1,x+14,y+6),fill=light,outline=dark)
    elif kind=='crystal':
        px(d,[(x+8,y+1),(x+13,y+6),(x+9,y+14),(x+4,y+9),(x+5,y+4)],'#54C4BC'); d.line((x+8,y+2,x+8,y+12),fill=cream)
    elif kind=='physical':
        d.line((x+4,y+12,x+12,y+4),fill=cream,width=2); d.line((x+3,y+10,x+6,y+13),fill=wood,width=2); d.line((x+10,y+3,x+13,y+6),fill=gold,width=2)
    elif kind=='water':
        px(d,[(x+8,y+1),(x+13,y+8),(x+12,y+12),(x+8,y+15),(x+4,y+12),(x+3,y+8)],'#4EAFE3'); d.point((x+6,y+8),fill=cream)
    elif kind=='fire':
        px(d,[(x+8,y+1),(x+11,y+6),(x+14,y+9),(x+12,y+14),(x+7,y+15),(x+3,y+12),(x+5,y+8)],'#E56B3D'); px(d,[(x+8,y+7),(x+10,y+11),(x+7,y+13),(x+6,y+10)],gold)
    elif kind=='acid':
        d.ellipse((x+3,y+3,x+12,y+13),fill='#A1D92F',outline='#642F84'); d.point((x+6,y+7),fill='#642F84'); d.point((x+10,y+5),fill=cream)

def framed_icon(kind, pressed=False, disabled=False):
    im=canvas(16,16); d=ImageDraw.Draw(im); icon(d,0,0,kind)
    if disabled:
        rgba=im.getdata(); im.putdata([(r//2,g//2,b//2,a) for r,g,b,a in rgba])
    return im

# Hotbar ornamental frame 28x112 and 24x24 4-state slots.
im=canvas(28,112); d=ImageDraw.Draw(im)
d.rectangle((5,3,22,108),fill='#392C23',outline='#806044')
d.line((3,3,3,107),fill='#4A813C',width=2); d.line((24,4,24,106),fill='#24552F',width=2)
for y in (9,30,53,76,99):
    d.line((2,y,7,y+4),fill='#78B94B',width=2); d.point((1,y-2),fill='#A4DB5A')
    d.line((25,y+8,21,y+3),fill='#39733C',width=2)
save('hud_hotbar_frame',im)
im=canvas(96,24)
for i in range(4):
    d=ImageDraw.Draw(im); x=i*24
    d.rectangle((x+1,1,x+22,22),fill=('#54462B' if i==1 else '#493629'),outline=('#E6BF62' if i==1 else '#9B7042'))
    d.rectangle((x+3,3,x+20,20),outline=('#F5DC83' if i==1 else '#2E5337'))
    if i==2: d.rectangle((x+2,2,x+21,21),fill='#281E23')
    if i==3:
        # small vine lock icon, deliberately pixel-blocked
        d.rectangle((x+10,11,x+16,17),fill='#33543A',outline='#9EBB68'); d.arc((x+11,6,x+16,13),180,360,fill='#9EBB68',width=2)
save('hud_slot',im)

# Health and mana atlases.
im=canvas(36,9); d=ImageDraw.Draw(im)
for i in range(4):
    x=i*9
    if i in (0,1,3):
        px(d,[(x+4,1),(x+2,0),(x,1),(x,4),(x+4,8),(x+8,4),(x+8,1),(x+6,0)],'#B83E42' if i!=3 else '#F06E54')
        d.point((x+2,2),fill='#F07A63')
    if i==1: px(d,[(x+4,2),(x+2,1),(x+1,2),(x+1,4),(x+4,7)],'#B83E42')
    if i==2:
        px(d,[(x+4,1),(x+2,0),(x,1),(x,4),(x+4,8),(x+8,4),(x+8,1),(x+6,0)],'#59393A')
save('hud_heart',im)
im=canvas(27,9); d=ImageDraw.Draw(im)
for i in range(3):
    x=i*9; px(d,[(x+4,0),(x+8,4),(x+7,7),(x+4,8),(x+1,6),(x,3)],'#39A957' if i!=2 else '#425844')
    if i==0: d.line((x+4,2,x+4,6),fill='#D1D56A'); d.point((x+2,3),fill='#8BDA68')
    if i==1: px(d,[(x+4,1),(x+7,4),(x+6,6),(x+4,7)],'#39A957'); d.line((x+4,2,x+4,6),fill='#D1D56A')
    if i==2: d.point((x+3,3),fill='#637864')
save('hud_mana',im)

# 9-slice strip (left, middle, right at x0..8; fill sample x9..9; level badge x10..22).
def bar_atlas(name, color):
    im=canvas(23,13); d=ImageDraw.Draw(im)
    d.rectangle((1,0,7,4),fill='#352B22'); d.rectangle((2,1,6,3),fill='#49372A')
    d.point((0,2),fill='#83613E'); d.point((8,2),fill='#83613E')
    for x in (1,2,3,5,6,7): d.point((x,1),fill='#6B5634')
    d.rectangle((9,0,9,2),fill=color)
    # level seal on lower portion
    d.ellipse((10,0,22,12),fill='#54452C',outline='#D5AE58'); d.ellipse((12,2,20,10),fill='#80663A',outline='#F0D17A')
    px(d,[(16,3),(19,6),(16,9),(13,6)],'#E9CA68'); d.point((16,6),fill='#FFF0A0')
    save(name,im)
bar_atlas('hud_xp_bar','#78CE58'); bar_atlas('hud_posture_bar','#DEA94C')

# Round button states: 32x32 cells.
im=canvas(96,32)
for i in range(3):
    d=ImageDraw.Draw(im); x=i*32
    base=['#647646','#76633D','#3C4D3B'][i]; edge=['#B2C875','#E1C579','#5D7054'][i]
    d.ellipse((x+1,1,x+30,30),fill='#2C3328',outline=edge,width=2); d.ellipse((x+4,4,x+27,27),fill=base,outline='#384333')
    d.point((x+9,6),fill='#C0D78A' if i==0 else '#8C9B68')
save('hud_button',im)

icon_kinds=['attack','special','swap','shield','potion','talk','attributes','bag','map','quest','menu','close']
im=canvas(16*len(icon_kinds),16)
for i,k in enumerate(icon_kinds): icon(ImageDraw.Draw(im),i*16,0,k)
save('hud_icons',im)
weapons=['leaf','coconut','staff','crystal','physical','close']
im=canvas(16*len(weapons),16)
for i,k in enumerate(weapons):
    if k=='close': continue
    icon(ImageDraw.Draw(im),i*16,0,k)
save('hud_weapon_icons',im)
elements=['leaf','water','fire','acid','physical']; im=canvas(12*len(elements),12)
for i,k in enumerate(elements): icon(ImageDraw.Draw(im),i*12,0,k,12)
save('hud_element_icons',im)

# Joystick, two exact cells (48 and 24), sheet width72.
im=canvas(72,48); d=ImageDraw.Draw(im)
d.ellipse((1,1,46,46),fill='#354337',outline='#89A96A',width=2)
for x,y in ((24,6),(24,42),(6,24),(42,24)): d.point((x,y),fill='#B2C97A')
d.ellipse((52,4,71,23),fill='#61754A',outline='#D3D995',width=2); d.point((57,8),fill='#E6E4AF')
save('hud_joystick',im)

# Wood/leaves panel 9-slice: one 24x24 tile with 8px corners.
im=canvas(24,24); d=ImageDraw.Draw(im)
d.rectangle((0,0,23,23),fill='#3B2B22',outline='#9B7042',width=2)
d.rectangle((3,3,20,20),fill='#28392D',outline='#6E5236',width=1)
for x,y in ((2,7),(21,16),(7,2),(16,21)):
    d.line((x,y,x+2,y+1),fill='#4E8D42',width=2); d.point((x+2,y+1),fill='#A3D35B')
for x,y in ((4,4),(19,4),(4,19),(19,19)): d.point((x,y),fill='#D2A75B')
save('hud_panel',im)

# Build an assembled 1200x540 review image with the actual one-times HUD sprites at 3x.
W,H=1200,540; mock=Image.new('RGBA',(W,H),(54,135,58,255)); md=ImageDraw.Draw(mock)
def crop_asset(name, rect=None, scale=3):
    source=Image.open(OUT/f'{name}.png').convert('RGBA')
    if rect: source=source.crop(rect)
    return source.resize((source.width*scale,source.height*scale),Image.Resampling.NEAREST)
grass=Image.open(OUT/'tile_grass.png').convert('RGBA')
for yy in range(0,H,64):
    for xx in range(0,W,64):
        idx=((xx//64)*3+(yy//64)*5)%4
        mock.alpha_composite(grass.crop((idx*64,0,idx*64+64,64)),(xx,yy))
trees=Image.open(OUT/'obj_tree_round.png').convert('RGBA')
for xx,yy in ((0,0),(170,0),(460,0),(930,0),(1060,20),(0,370),(190,405),(430,390),(850,390),(1070,380)):
    mock.alpha_composite(trees.crop((0,0,128,128)),(xx,yy))
player=Image.open(OUT/'player_idle.png').convert('RGBA')
mock.alpha_composite(player.crop((0,0,64,64)),(568,237))

def put(name, x, y, rect=None, scale=3): mock.alpha_composite(crop_asset(name,rect,scale),(int(x),int(y)))
def nine_panel(x,y,w,h,scale=3):
    sheet=Image.open(OUT/'hud_panel.png').convert('RGBA'); k=8
    for row in range(3):
        for col in range(3):
            sx=(0,k,24-k,24); sy=(0,k,24-k,24)
            dw=(k*scale, max(1,w-2*k*scale), k*scale)[col]
            dh=(k*scale, max(1,h-2*k*scale), k*scale)[row]
            piece=sheet.crop((sx[col],sy[row],sx[col+1],sy[row+1])).resize((dw,dh),Image.Resampling.NEAREST)
            mock.alpha_composite(piece,(x+(0,k*scale,w-k*scale)[col],y+(0,k*scale,h-k*scale)[row]))

# Health, mana and level/XP panel.
nine_panel(9,7,288,108)
for i in range(5): put('hud_heart',21+i*33,16,(0,0,9,9))
for i in range(7): put('hud_mana',21+i*33,52,(0,0,9,9))
put('hud_xp_bar',21,80,(0,0,9,5)); put('hud_xp_bar',48,80,(4,0,5,5)); put('hud_xp_bar',228,80,(5,0,9,5))
put('hud_xp_bar',30,86,(9,0,10,3)); put('hud_xp_bar',222,68,(10,0,23,13))
hot=Image.open(OUT/'hud_hotbar_frame.png').convert('RGBA').resize((84,336),Image.Resampling.NEAREST)
mock.alpha_composite(hot,(9,102))
for i,kind in enumerate(('coconut','staff','crystal','physical')):
    put('hud_slot',15,114+i*75,(24 if i==0 else 0,0,48 if i==0 else 24,24))
    weapon_index=('coconut','staff','crystal','physical').index(kind)
    put('hud_weapon_icons',21+i*75,126+i*75,(weapon_index*16,0,weapon_index*16+16,16))

# Joystick and three round action controls.
put('hud_joystick',144,351,(0,0,48,48),3)
put('hud_joystick',176,383,(48,0,72,24),3)
def button(x,y,icon_index,state=0):
    put('hud_button',x-48,y-48,(state*32,0,state*32+32,32))
    put('hud_icons',x-24,y-24,(icon_index*16,0,icon_index*16+16,16))
button(1080,464,0); button(924,464,1); button(1056,356,2)

# Compact navigation strip; the real Android pause affordance sits just to its right.
for i,icon_index in enumerate((6,7,8,9)):
    x=408+i*144
    nine_panel(x+18,15,108,108)
    put('hud_icons',x+42,39,(icon_index*16,0,icon_index*16+16,16))
button(1140,48,10)
mock.convert('RGB').save(DOCS/'hud_mockup.png')
print('Generated HUD sheets and docs/hud_mockup.png')
