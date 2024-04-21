def A():
    print("A")

def B():
    print("B")
    A()

def C():
    A() # A being called by C, not B