from PIL import Image
im=Image.open(r"shots/dynamics/_r9full_009.png").convert('RGB')
print(im.size)
im.crop((0,0,1080,760)).save(r"_f9_top.png")
im2=Image.open(r"shots/dynamics/_r9full_018.png").convert('RGB')
im2.crop((0,0,1080,760)).save(r"_f18_top.png")
