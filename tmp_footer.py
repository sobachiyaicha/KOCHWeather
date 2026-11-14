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
L=(a@np.array([0.2126,0.7152,0.0722]))
# text bbox search in band
band=L[1150:1230,200:900]
ys,xs=np.where(band>200)
print("text pixels:",len(ys),"y range",ys.min()+1150,ys.max()+1150,"x",xs.min()+200,xs.max()+200)
sub=a[1150:1230,200:900].reshape(-1,3)
Ls=L[1150:1230,200:900].reshape(-1)
fg=sub[Ls>=np.percentile(Ls,99)].mean(axis=0)
bg=sub[(Ls>70)&(Ls<90)].mean(axis=0)
print("footer fg",np.round(fg).astype(int),"bg",np.round(bg).astype(int),"CR",round(cr(rel_lum(fg),rel_lum(bg)),2))
# pill extents
col=L[:,150:250].mean(axis=1)
rows=np.where((L[:,500]>70)&(L[:,500]<95))[0]
print("pill rows approx",rows.min(),rows.max())
# background around pill
print("bg above pill", np.round(a[1100,500]).astype(int), "bg below pill", np.round(a[1260,500]).astype(int), "copper corner",np.round(a[1500,540]).astype(int))
