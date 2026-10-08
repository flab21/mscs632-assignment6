# MSCS-632 Assignment 6: Multi-threaded Data Processing System

Frenie Labrador, University of the Cumberlands, MSCS-632-A01

Same system built twice. Worker threads pull tasks from a shared queue, "process" each one (sleep 100-399 ms, then square the number), and save results to a shared store and an output file. Tasks 7 and 15 have negative data on purpose so the error handling shows up in the logs.

## Java (`java/`)

Shared queue is a `LinkedList` guarded by a `ReentrantLock` with a `Condition` so idle workers sleep. Workers run on an `ExecutorService` fixed pool of 4. Errors are handled with try-catch (`InterruptedException`, `IOException`, `NoSuchElementException` for empty queue, `IllegalArgumentException` for bad data).

```
cd java
javac *.java
java Main
```

Output goes to `java_results.txt`. To see the file I/O error handling, give it a bad path: `java Main /nope/out.txt`

## Go (`go/`)

Shared queue is a buffered channel. 4 goroutines read from it, a `sync.WaitGroup` waits for them, and a `sync.Mutex` protects the result store. Errors are returned values that get checked, and `defer` handles cleanup (`wg.Done`, `mu.Unlock`, `f.Close`, panic recovery).

```
cd go
go run .
```

Output goes to `go_results.txt`. Bad path test: `go run . /nope/out.txt`
Race detector check: `go run -race .`

## Requirements

Java 17+ and Go 1.21+
