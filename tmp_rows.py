import numpy as np
from PIL import Image
im=Image.open(r"shots\r9_05_list_footer.png").convert('RGB')
a=np.asarray(im).astype(float)
print("size",im.size)
L=(a@np.array([0.2126,0.7152,0.0722]))
# row profile center band
for y in range(1000,1450,20):
    row=L[y,100:980]
    print(y, int(row.mean()), int(np.percentile(row,10)), int(np.percentile(row,90)))
