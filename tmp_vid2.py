import cv2, numpy as np
p=r"shots\dynamics\r9_clouds.mp4"
cap=cv2.VideoCapture(p)
rows=[]
i=0
while True:
    ok,fr=cap.read()
    if not ok: break
    rgb=cv2.cvtColor(fr,cv2.COLOR_BGR2RGB).astype(int)
    L=(rgb@np.array([0.2126,0.7152,0.0722]))
    panel=L[120:380,20:60].mean()            # veil panel left edge luminance
    hint_dark=(L[220:390,180:900]<110).sum() # dark hint text pixels
    cap_yellow=((rgb[170:430,:,0]>190)&(rgb[170:430,:,1]>150)&(rgb[170:430,:,2]<130)).sum()
    cap_text=(L[190:430,80:860]<90).sum()    # dark text in capsule zone (readable city name)
    rows.append((i,round(panel,0),hint_dark,cap_yellow,cap_text))
    i+=1
cap.release()
print(" frame panel tip_dark capY capTextDark")
for r in rows: print(f"{r[0]:5d} {r[1]:6.0f} {r[2]:9d} {r[3]:6d} {r[4]:8d}")
