def baz():
    print("baz invoked")

def bar():
    print("bar invoked")
    baz()

def intermediary():
    print("intermediary invoked")
    bar()

def foo(): # will transitively invoke bar and baz
    print("foo invoked")
    intermediary()
