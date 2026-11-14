import numpy as np
from PIL import Image

def lin(c):
    c=np.asarray(c,dtype=np.float64)/255.0
    return np.where(c<=0.04045,c/12.92,((c+0.055)/1.055)**2.4)

def rel_lum(rgb):
    rgb=np.asarray(rgb,dtype=np.float64)
    r,g,b=lin(rgb[...,0]),lin(rgb[...,1]),lin(rgb[...,2])
    return 0.2126*r+0.7152*g+0.0722*b

def cr(l1,l2):
    a,b=max(l1,l2),min(l1,l2)
    return (a+0.05)/(b+0.05)

def contrast_region(img, box, label, dark_text=True, pct=4, bgpct=60):
    a=np.asarray(img.crop(box).convert('RGB'))
    flat=a.reshape(-1,3); L=rel_lum(a).reshape(-1)
    if dark_text:
        fg=flat[L<=np.percentile(L,pct)].mean(axis=0)
        bg=flat[L>=np.percentile(L,bgpct)].mean(axis=0)
    else:
        fg=flat[L>=np.percentile(L,100-pct)].mean(axis=0)
        bg=flat[L<=np.percentile(L,100-bgpct)].mean(axis=0)
    print(f"{label:40s} fg={np.round(fg).astype(int)} bg={np.round(bg).astype(int)} CR={cr(rel_lum(fg),rel_lum(bg)):.2f}:1")

im7=Image.open(r"shots\r9_07_veil.png")
print("== r9_07 veil: DARK icons on light panel ==")
contrast_region(im7,(480,15,600,85),"clock 10:15")
contrast_region(im7,(60,15,360,85),"left icons")
contrast_region(im7,(800,15,1040,85),"right icons")
print("== r9_07 hint text on panel ==")
contrast_region(im7,(200,240,880,380),"hint two lines")
contrast_region(im7,(500,180,580,240),"touch icon")

im5=Image.open(r"shots\r9_05_list_footer.png")
print("== r9_05 footer ==")
# find footer pill rows
a=np.asarray(im5.convert('RGB')).astype(int)
# footer pill approx y 1120..1195, x 100..980 (from preview 1080x1920 scale: 1:1)
contrast_region(im5,(310,1130,770,1175),"footer text 'Данные: ...'",dark_text=False)
contrast_region(im5,(200,1140,270,1170),"footer pill left area")
