class A:
    def a():
        return 1
def main():
    return 1

class B:
    def b():
        return 1
    
class C(B):
    def b():
        return 1
    def main():
        return 1

class D(C):
    def b():
        return 1
    
class E(D):
    def b():
        return 1

