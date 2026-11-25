package goopt

import "testing"

var data = make([]byte, 1024)

func BenchmarkHashNaive(b *testing.B) {
	for i := 0; i < b.N; i++ {
		Hash(data)
	}
}

var sink uint32

func BenchmarkHashSink(b *testing.B) {
	for i := 0; i < b.N; i++ {
		sink = Hash(data)
	}
}

func BenchmarkHashLoop(b *testing.B) {
	for b.Loop() {
		Hash(data)
	}
}
