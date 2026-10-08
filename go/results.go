package main

import (
	"bufio"
	"fmt"
	"log"
	"os"
	"sync"
)

// ResultStore is the shared resource the workers write into.
// A mutex makes sure only one goroutine changes it at a time.
type ResultStore struct {
	mu       sync.Mutex
	results  []string
	failures []string
	seen     map[int]bool
}

func NewResultStore() *ResultStore {
	return &ResultStore{seen: make(map[int]bool)}
}

func (s *ResultStore) AddResult(t Task, value int, worker int) {
	s.mu.Lock()
	defer s.mu.Unlock() // unlock runs even if something below panics
	s.checkDuplicate(t.ID)
	s.results = append(s.results,
		fmt.Sprintf("Task %d | input %d | result %d | by worker-%d", t.ID, t.Data, value, worker))
}

func (s *ResultStore) AddFailure(t Task, reason error, worker int) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.checkDuplicate(t.ID)
	s.failures = append(s.failures,
		fmt.Sprintf("Task %d | FAILED: %v | by worker-%d", t.ID, reason, worker))
}

// caller must already hold the lock
func (s *ResultStore) checkDuplicate(id int) {
	if s.seen[id] {
		log.Printf("[ERROR] [main] duplicate result for task %d, this should never happen", id)
	}
	s.seen[id] = true
}

func (s *ResultStore) Counts() (ok int, failed int, unique int) {
	s.mu.Lock()
	defer s.mu.Unlock()
	return len(s.results), len(s.failures), len(s.seen)
}

// WriteToFile saves everything to disk. Every step that can fail is
// checked, and the deferred Close makes sure the file is released.
// err is a named return so the deferred function can report a Close error.
func (s *ResultStore) WriteToFile(path string) (err error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	f, err := os.Create(path)
	if err != nil {
		return fmt.Errorf("create %s: %w", path, err)
	}
	defer func() {
		if cerr := f.Close(); cerr != nil && err == nil {
			err = fmt.Errorf("close %s: %w", path, cerr)
		}
	}()

	w := bufio.NewWriter(f)
	lines := []string{"=== Go Data Processing Results ==="}
	lines = append(lines, s.results...)
	lines = append(lines, "=== Failed Tasks ===")
	lines = append(lines, s.failures...)

	for _, line := range lines {
		if _, err := w.WriteString(line + "\n"); err != nil {
			return fmt.Errorf("write %s: %w", path, err)
		}
	}
	if err := w.Flush(); err != nil {
		return fmt.Errorf("flush %s: %w", path, err)
	}
	return nil
}
