def foo():
    f = open()
    f.close()

def bar():
    f = open()

def bar1():
    f = open()
    g = f
    g.close() 

def bar2():
    f = open()
    g = open()
    a = g
    b = f
    a.close()
    b.close()

def bar3():
    f = open()
    g = open()
    f = g
    f.close()