class Foo:
    def foo():
        return 0
class Baz(Foo):
    def baz():
        return 1
    def foo():
        return 0

class Bar(Baz):
    def foo():
        return 0
    def baz():
        return 1
    def bar():
        return 2