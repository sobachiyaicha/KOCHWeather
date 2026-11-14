from PIL import Image
im=Image.open(r"shots\r9_06_lootbox_FAIL_sheet_not_opened.png").convert('RGB')
im.crop((40,1150,1040,1560)).resize((1000,410),Image.LANCZOS).save(r"_loot_empty2x.png")
im.crop((40,1150,1040,1560)).resize((2000,820),Image.LANCZOS).save(r"_loot_empty2x_b.png")
print("ok")
