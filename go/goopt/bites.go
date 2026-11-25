package goopt

var ready bool

func WaitReady() {
	for !ready {
	}
}

func Delay() {
	for i := 0; i < 1000000; i++ {
	}
}

//go:noinline
func readPassword(pw []byte) {
	copy(pw, "hunter2")
}

//go:noinline
func check(pw []byte) bool {
	return string(pw[:7]) == "hunter2"
}

func Login() bool {
	var pw [64]byte
	readPassword(pw[:])
	ok := check(pw[:])
	clear(pw[:])
	return ok
}
