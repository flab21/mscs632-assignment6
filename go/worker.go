package main

import (
	"errors"
	"fmt"
	"log"
	"math/rand"
	"sync"
	"time"
)

// worker keeps pulling from the queue until GetTask says it's empty.
func worker(id int, q *TaskQueue, store *ResultStore, wg *sync.WaitGroup) {
	// defers run in reverse order: recover first, then wg.Done
	defer wg.Done()
	defer func() {
		// if anything panics, log it instead of crashing the whole program
		if r := recover(); r != nil {
			log.Printf("[ERROR] [worker-%d] panic recovered: %v", id, r)
		}
	}()

	log.Printf("[INFO ] [worker-%d] worker started", id)
	processed := 0

	for {
		task, err := q.GetTask()
		if errors.Is(err, ErrQueueEmpty) {
			log.Printf("[INFO ] [worker-%d] queue is empty and closed, no more work", id)
			break
		}

		value, err := process(task)
		if err != nil {
			log.Printf("[ERROR] [worker-%d] could not process %v: %v", id, task, err)
			store.AddFailure(task, err, id)
			continue
		}

		store.AddResult(task, value, id)
		processed++
		log.Printf("[INFO ] [worker-%d] finished %v -> %d", id, task, value)
	}

	log.Printf("[INFO ] [worker-%d] worker completed, processed %d task(s)", id, processed)
}

// process fakes some real work: sleep, then square the number.
// It returns an error instead of throwing, which is the Go way.
func process(t Task) (int, error) {
	time.Sleep(time.Duration(100+rand.Intn(300)) * time.Millisecond)
	if t.Data < 0 {
		return 0, fmt.Errorf("negative input %d", t.Data)
	}
	return t.Data * t.Data, nil
}
