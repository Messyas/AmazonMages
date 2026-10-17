from PIL import Image, ImageDraw
from pathlib import Path
import math, random, struct, wave

OUT = Path('app/src/main/res/drawable-nodpi')
RAW = Path('app/src/main/res/raw')
OUT.mkdir(parents=True, exist_ok=True)
RAW.mkdir(parents=True, exist_ok=True)

COLORS = {
    'outline': '#3B2923', 'shadow': '#503329', 'dark': '#65402B',
    'mid': '#93603A', 'light': '#BD8550', 'shine': '#E0B878',
    'eye': '#332823', 'gold': '#E8C45F', 'leaf': '#4A9C45',
}

def transparent(w,h): return Image.new('RGBA',(w,h),(0,0,0,0))

def fruit_frame(angle=0, size=32):
    # Draw the fruit on a 24px logical canvas, with the three husk eyes as its face.
    base=transparent(24,24); d=ImageDraw.Draw(base)
    silhouette=[(5,7),(8,4),(14,3),(19,6),(21,10),(20,16),(17,20),(12,21),(7,19),(4,15),(3,11)]
    d.polygon(silhouette, fill=COLORS['mid'], outline=COLORS['outline'])
    d.polygon([(5,10),(8,6),(11,5),(9,12),(8,18),(5,15),(4,12)], fill=COLORS['light'])
    d.polygon([(11,5),(15,5),(13,10),(12,14),(11,19),(8,18),(9,12)], fill=COLORS['mid'])
    d.polygon([(16,6),(19,8),(20,12),(18,17),(15,19),(13,18),(14,12)], fill=COLORS['dark'])
    d.line([(6,8),(9,6),(11,6)], fill=COLORS['shine'], width=1)
    d.line([(6,11),(7,15)], fill='#D39A5D', width=1)
    # Three coconut eyes form the recognizable triangular scar at the lower face.
    d.point((11,15), fill=COLORS['eye']); d.point((14,15), fill=COLORS['eye']); d.point((12,17), fill=COLORS['eye'])
    d.point((10,14), fill=COLORS['shine']); d.point((13,14), fill='#C99A66')
    # Short stem and one pointed green leaflet; the coconut remains the dominant silhouette.
    d.line((14,4,15,2), fill=COLORS['shadow'], width=2)
    d.polygon([(15,3),(18,1),(18,4),(16,5)], fill=COLORS['leaf'])
    d.point((17,2), fill='#91D05A')
    # Nearest-neighbor rotation keeps the edges hard while giving the flight a readable spin.
    base=base.rotate(angle, resample=Image.Resampling.NEAREST, expand=False)
    scale=size//24
    if scale*24 != size:
        return base.resize((size,size),Image.Resampling.NEAREST)
    return base.resize((size,size),Image.Resampling.NEAREST)

def save_sheet(name, cols, cw, ch, draw_cell):
    sheet=transparent(cols*cw,ch)
    for i in range(cols):
        cell=draw_cell(i,cw,ch)
        sheet.alpha_composite(cell,(i*cw,0))
    sheet.save(OUT/f'{name}.png')

def projectile(i,w,h):
    angles=(0,30,70,120)
    fruit=fruit_frame(angles[i],24)
    cell=transparent(w,h); cell.alpha_composite(fruit,((w-24)//2,(h-24)//2))
    return cell

def summon(i,w,h):
    cell=transparent(w,h); d=ImageDraw.Draw(cell)
    fruit=fruit_frame((0,10,25,50,0)[i],20)
    # The casting hand's glow accumulates, peaks as the fruit forms, then releases.
    dots=[[(15,16)],[(11,15),(20,12)],[(10,10),(21,10),(15,23)],[(7,13),(22,8),(24,20)],[(5,8),(25,8),(16,25)]]
    for j,(x,y) in enumerate(dots[i]):
        col=COLORS['gold'] if (j+i)%2==0 else '#82D75A'
        d.rectangle((x-1,y-1,x+1,y+1),fill=col)
    if i>=2: cell.alpha_composite(fruit,((w-20)//2,(h-20)//2))
    if i==4:
        d=ImageDraw.Draw(cell); d.point((15,5),fill='#F8ED9A'); d.point((8,19),fill='#A5E967')
    return cell

def fall(i,w,h):
    cell=transparent(w,h); d=ImageDraw.Draw(cell)
    fruit=fruit_frame((0,25,55,90)[i],22)
    # Short hard-edged air streaks, offset behind the spinning fruit.
    if i in (0,2):
        d.line((9,2,9,8),fill='#E7D994',width=1); d.line((13,0,13,5),fill='#83C75D',width=1)
    cell.alpha_composite(fruit,((w-22)//2,10))
    return cell

def shadow(i,w,h):
    cell=transparent(w,h); d=ImageDraw.Draw(cell)
    pulse=(0,1,2,1)[i]
    d.ellipse((4-pulse,10-pulse,27+pulse,21+pulse),fill='#362F26',outline='#81613B',width=1)
    d.ellipse((8,12,23,19),outline=COLORS['gold'],width=1)
    for x,y in ((5,12),(25,17),(12,9),(19,21)):
        d.point((x,y),fill=COLORS['leaf'])
    d.point((16,15),fill='#D29C4F')
    return cell

def impact(i,w,h):
    cell=transparent(w,h); d=ImageDraw.Draw(cell)
    cx=cy=24
    if i<=3:
        r=(4,8,13,17)[i]
        d.ellipse((cx-r,cy-r//2,cx+r,cy+r//2),outline='#A87948',width=1)
        if i>=1: d.ellipse((cx-r+2,cy-r//2+1,cx+r-2,cy+r//2-1),outline='#D4B16D',width=1)
    for j in range(7):
        angle=(j*2*math.pi/7)+(i*.12); distance=3+i*2
        x=round(cx+math.cos(angle)*distance); y=round(cy+math.sin(angle)*distance*.55)
        if i<5:
            col=(COLORS['light'],COLORS['gold'],COLORS['dark'])[j%3]
            d.rectangle((x,y,x+2,y+1),fill=col)
    if i<4:
        d.polygon([(20,18),(24,14),(28,17),(27,22),(22,25),(18,22)],fill=COLORS['mid'],outline=COLORS['outline'])
        d.point((23,19),fill=COLORS['eye']); d.point((25,19),fill=COLORS['eye'])
    return cell

save_sheet('coconut_projectile',4,32,32,projectile)
save_sheet('coconut_summon',5,32,32,summon)
save_sheet('coconut_fall',4,32,40,fall)
save_sheet('coconut_shadow',4,32,32,shadow)
save_sheet('coconut_impact',6,48,48,impact)

def write_wav(name, duration, sample_fn):
    rate=22050; count=int(rate*duration)
    samples=[]
    for n in range(count):
        sample=max(-1,min(1,sample_fn(n/rate)))
        samples.append(int(sample*24000))
    with wave.open(str(RAW/name),'wb') as out:
        out.setnchannels(1); out.setsampwidth(2); out.setframerate(rate)
        out.writeframes(b''.join(struct.pack('<h',s) for s in samples))

# Original synthesized basic-hit thump: fast wooden transient, low body, short noisy tail.
def coconut_hit(t):
    rng_state = int(t*22050)
    rng = random.Random(519 + rng_state)
    env=math.exp(-t*19); freq=118-180*t
    body=math.sin(2*math.pi*freq*t)+.27*math.sin(2*math.pi*freq*2.07*t)
    crack=(rng.random()*2-1)*math.exp(-t*52) if t<.075 else 0
    return (body*.58+crack*.42)*env

write_wav('sfx_coconut_hit.wav',.24,coconut_hit)

# Storm conjuration: a rising airy sweep with three bright, hand-played style chimes.
def storm_cast(t):
    duration=.9; env=min(1,t/.08)*math.exp(-t*1.65)
    sweep=math.sin(2*math.pi*(112*t+92*t*t))*.21
    noise=(random.Random(int(t*22050)+807).random()*2-1)*math.sin(min(1,t/.35)*math.pi/2)*math.exp(-t*2.5)*.12
    chime=0
    for start,freq in ((.18,523.25),(.34,659.25),(.52,783.99)):
        age=t-start
        if age>=0: chime+=math.sin(2*math.pi*freq*age)*math.exp(-age*10)*.16
    return (sweep+noise+chime)*env

write_wav('sfx_coconut_storm_cast.wav',.9,storm_cast)

# Storm impacts: shell-like double knock with a small gritty snap, shorter than the cast.
def storm_hit(t):
    rng=random.Random(2026+int(t*22050))
    knock=0
    for start,amp,freq in ((0,.62,146),(.065,.34,104)):
        age=t-start
        if age>=0: knock+=amp*math.sin(2*math.pi*(freq-90*age)*age)*math.exp(-age*20)
    snap=(rng.random()*2-1)*math.exp(-t*58)*.23 if t<.09 else 0
    tail=math.sin(2*math.pi*480*t)*math.exp(-t*24)*.08
    return knock+snap+tail

write_wav('sfx_coconut_storm_hit.wav',.28,storm_hit)
print('Generated coconut atlases and coconut basic/storm sound effects')
