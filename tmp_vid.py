import cv2, numpy as np, os
p=r"shots\dynamics\r9_clouds.mp4"
cap=cv2.VideoCapture(p)
n=int(cap.get(cv2.CAP_PROP_FRAME_COUNT)); fps=cap.get(cv2.CAP_PROP_FPS)
w=int(cap.get(cv2.CAP_PROP_FRAME_WIDTH)); h=int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
print("frames",n,"fps",round(fps,2),"dur",round(n/max(fps,1e-6),2),"size",w,h)
prev=None; idx=0; uniq=[]
hashes=[]
while True:
    ok,fr=cap.read()
    if not ok: break
    small=cv2.resize(fr,(54,96))
    g=cv2.cvtColor(small,cv2.COLOR_BGR2GRAY).astype(np.int16)
    if prev is None or np.abs(g-prev).mean()>0.6:
        uniq.append(idx); cv2.imwrite(f"shots/dynamics/_r9f_{idx:03d}.png",fr)
    prev=g; idx+=1
print("distinct:",len(uniq), uniq)
cap.release()
