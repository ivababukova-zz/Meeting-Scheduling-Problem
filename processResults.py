import sys
with open(sys.argv[1]) as re:
    lista = re.readlines()
    print("avg for ", sys.argv[1], " " , sum([float(elem) for elem in lista])/len(lista))
