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

im5=Image.open(r"shots\r9_05_list_footer.png").convert('RGB')
a=np.asarray(im5).astype(float)
fg=np.array([242,238,237]); bg=np.array([88,34,25])
print("r9_05 footer (copper): CR", round(cr(rel_lum(fg),rel_lum(bg)),2))

# light-list worst case from r7_16 (blue theme)
im16=Image.open(r"shots\r7_16_list.png").convert('RGB')
b=np.asarray(im16).astype(float)
print("r7_16 size",im16.size)
# locate footer band
L=(b@np.array([0.2126,0.7152,0.0722]))
for y in range(1000,1400,25):
    print(y, [tuple(np.round(b[y,x]).astype(int)) for x in [100,300,540,800,1000]])
