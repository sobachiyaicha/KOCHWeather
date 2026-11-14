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
    a,b=max(l1,l2),min(l1,l2); return (a+0.05)/(b+0.05)
im=Image.open(r"shots\r9_05_list_footer.png").convert('RGB')
a=np.asarray(im).astype(float)
for y in [1110,1125,1140,1155,1170,1185,1200,1215,1230,1245]:
    print(y, [tuple(np.round(a[y,x]).astype(int)) for x in [120,200,500,700,950]])
text=np.array([242,238,237])
pill=np.round(a[1140,500]).astype(int)
print("pill color",pill,"CR white-text vs pill",round(cr(rel_lum(text),rel_lum(pill)),2))
# darkest/brightest inside pill excluding text
reg=a[1130:1145,300:780].reshape(-1,3)
med=np.median(reg,axis=0); print("median pill",np.round(med).astype(int),"CR",round(cr(rel_lum(text),rel_lum(med)),2))
