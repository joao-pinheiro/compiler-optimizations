package goopt

func SecondsPerWeek() int {
	minutes := 60
	hours := 24 * minutes
	return 7 * hours * 60
}

func square(x int) int { return x * x }

func SumSquares(a, b int) int { return square(a) + square(b) }

func Div10(x uint32) uint32 { return x / 10 }

func Hash(s []byte) uint32 {
	var h uint32
	for _, c := range s {
		h = h*31 + uint32(c)
	}
	return h
}

func Add(dst, a, b []int32) {
	for i := range dst {
		dst[i] = a[i] + b[i]
	}
}

func AddBCE(dst, a, b []int32) {
	if len(a) < len(dst) || len(b) < len(dst) {
		panic("short input")
	}
	for i := range dst {
		dst[i] = a[i] + b[i]
	}
}

func SumTo(n uint32) uint32 {
	var s uint32
	for i := uint32(1); i <= n; i++ {
		s += i
	}
	return s
}

func Fact(n, acc uint64) uint64 {
	if n <= 1 {
		return acc
	}
	return Fact(n-1, acc*n)
}

type Point struct{ X, Y int }

func NewPointStack() int {
	p := &Point{1, 2}
	return p.X + p.Y
}

func NewPointHeap() *Point {
	return &Point{1, 2}
}

const debug = false

func F(x int) int {
	if debug {
		println("x =", x)
	}
	return x + 1
}
