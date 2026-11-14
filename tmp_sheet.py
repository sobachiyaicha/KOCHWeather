import cv2, numpy as np
p=r"shots\dynamics\r9_clouds.mp4"
want=[0,1,2,4,7,9,10,18,24,25,26,38]
cap=cv2.VideoCapture(p); frames={}
i=0
while True:
    ok,fr=cap.read()
    if not ok: break
    if i in want: frames[i]=fr.copy()
    i+=1
cap.release()
# tiles of top 700px only (veil/capsule zone) at 1/2
tiles=[]
for k in want:
    f=frames[k][0:760,:]
    t=cv2.resize(f,(540,380))
    cv2.putText(t,f"f{k}",(8,30),cv2.FONT_HERSHEY_SIMPLEX,1.0,(0,0,255),2)
    tiles.append(t)
rows=[np.hstack(tiles[i:i+3]) for i in range(0,12,3)]
cv2.imwrite(r"_delta_veil_sheet.png",np.vstack(rows))
for k in [0,2,9,18,38]:
    cv2.imwrite(rf"shots/dynamics/_r9full_{k:03d}.png",frames[k])
print("ok")
