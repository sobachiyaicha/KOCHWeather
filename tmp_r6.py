import numpy as np
from PIL import Image
im=Image.open(r"shots\r9_03_roulette.png").convert('RGB')
a=np.asarray(im).astype(int)
L=(a@np.array([0.2126,0.7152,0.0722]))
sub=L[380:1150,250:830]
mask=sub<45
ys,xs=np.where(mask)
print("black container bbox: x",xs.min()+250,xs.max()+250,"y",ys.min()+380,ys.max()+380,
      "w",xs.max()-xs.min()+1,"h",ys.max()-ys.min()+1)
w=xs.max()-xs.min()+1; h=ys.max()-ys.min()+1
print("px w,h",w,h,"dp w,h",round(w/3,1),round(h/3,1),"aspect",round(w/h,3),"target 118/160",round(118/160,3))
# phone photo (non-black inside container): find bright photo region
sub2=a[400:1100,380:720]
Lb=(sub2@np.array([0.2126,0.7152,0.0722]))
m2=Lb>45
ys2,xs2=np.where(m2)
print("photo inside bbox x",xs2.min()+380,xs2.max()+380,"y",ys2.min()+400,ys2.max()+400,
      "w",xs2.max()-xs2.min()+1,"h",ys2.max()-ys2.min()+1)
# yellow arrows
R,G,B=a[...,0],a[...,1],a[...,2]
yellow=(R>200)&(G>180)&(B<90)
ys3,xs3=np.where(yellow[600:1100,:])
print("yellow px count",len(ys3),"bbox x",xs3.min(),xs3.max(),"y",ys3.min()+600,ys3.max()+600)
# left/right clusters
left=xs3[xs3<540]; right=xs3[xs3>=540]
print("left arrow x",left.min(),left.max(),"right arrow x",right.min(),right.max())
print("yellow sample color",a[ (ys3[0]+600), xs3[0] ])
# status 'Доступно' green row and chips rows
green=(G>120)&(R<150)&(B<120)&(G-R>30)&(G-B>30)
ys4,xs4=np.where(green[1100:1400,80:1000])
print("green status bbox y",ys4.min()+1100,ys4.max()+1100,"x",xs4.min()+80,xs4.max()+80)
# chip outline rows: find the chip borders (white alpha) around y 1230..1330
for y in [1190,1210,1225,1240,1260,1300,1360,1400,1440]:
    row=a[y,100:1000]
    print(y, "mean", row.mean(axis=0).round(0))
