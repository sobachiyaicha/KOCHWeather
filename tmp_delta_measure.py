import numpy as np
from PIL import Image

def lin(c):
    c=np.asarray(c,dtype=np.float64)/255.0
    return np.where(c<=0.04045,c/12.92,((c+0.055)/1.055)**2.4)

def rel_lum(rgb):
    rgb=np.asarray(rgb,dtype=np.float64)
    r,g,b=rgb[...,0],rgb[...,1],rgb[...,2]
    r,g,b=lin(r),lin(g),lin(b)
    return 0.2126*r+0.7152*g+0.0722*b

def cr(l1,l2):
    a,b=max(l1,l2),min(l1,l2)
    return (a+0.05)/(b+0.05)

def bbox_measure(img, box, label, hi=97, lo=20):
    a=np.asarray(img.crop(box).convert('RGB'))
    L=rel_lum(a)
    flat=a.reshape(-1,3); Lf=L.reshape(-1)
    fgc=flat[Lf>=np.percentile(Lf,hi)].mean(axis=0)
    bgc=flat[Lf<=np.percentile(Lf,lo)].mean(axis=0)
    print(f"{label:34s} fg={np.round(fgc).astype(int)} bg={np.round(bgc).astype(int)} CR={cr(rel_lum(fgc),rel_lum(bgc)):.2f}:1")

def med_region(img, box, label):
    a=np.asarray(img.crop(box).convert('RGB')).reshape(-1,3)
    m=np.median(a,axis=0)
    print(f"{label:34s} median={np.round(m).astype(int)} L={rel_lum(m):.3f}")

im1=Image.open(r"shots\r9_01_main_blue_status.png")
im2=Image.open(r"shots\r9_02_main_copper_status.png")
im7=Image.open(r"shots\r9_07_veil.png")
im5=Image.open(r"shots\r9_05_list_footer.png")
old=Image.open(r"shots\r7_01_main_blue.png")

print("== r9_01 blue ==")
bbox_measure(im1,(480,20,600,80),"clock")
bbox_measure(im1,(60,20,360,80),"left icons")
bbox_measure(im1,(800,20,1040,80),"right icons")
print("== r9_02 copper ==")
bbox_measure(im2,(480,20,600,80),"clock")
bbox_measure(im2,(60,20,360,80),"left icons")
bbox_measure(im2,(800,20,1040,80),"right icons")
print("== r9_07 veil ==")
bbox_measure(im7,(480,20,600,80),"clock")
bbox_measure(im7,(60,20,360,80),"left icons")
print("== r7_01 old (for reference) ==")
bbox_measure(old,(480,20,600,80),"clock")
