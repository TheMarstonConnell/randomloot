import os
import shutil


def copyAll(path):
    items = os.listdir(path)
    for item in items:
        shutil.copyfile(path + "/" + item, path + item)
def main():
    copyAll("./pickaxe")
    copyAll("./axe")
    copyAll("./shovel")
    copyAll("./sword")
    
    



if __name__ == "__main__":
    main()