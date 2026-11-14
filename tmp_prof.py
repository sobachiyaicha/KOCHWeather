import numpy as np
from PIL import Image
im=Image.open(r"shots\r9_03_roulette.png").convert('RGB')
a=np.asarray(im).astype(int)
print("== horizontal profile y=700 ==")
prev=None
for x in range(300,800,2):
    c=tuple(a[700,x])
    if prev is None or (abs(c[0]-prev[0])+abs(c[1]-prev[1])+abs(c[2]-prev[2]))>40:
        print(x,c); prev=c
print("== vertical profile x=540 ==")
prev=None
for y in range(440,1020,2):
    c=tuple(a[y,540])
    if prev is None or (abs(c[0]-prev[0])+abs(c[1]-prev[1])+abs(c[2]-prev[2]))>40:
        print(y,c); prev=c
