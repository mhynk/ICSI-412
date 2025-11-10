# Java Operating System Simulator

### A full-featured educational OS implemented from scratch in Java

---

## Overview
**Java Operating System Simulator** is a fully functional educational OS built entirely in Java, designed to model the **core components of modern operating systems** within the JVM.  
The project demonstrates **process scheduling, device management, interprocess communication (IPC), and memory paging** using an object-oriented architecture.

---

## Key Features

### Kernel & Scheduler  
- Designed a **cooperative multitasking system** supporting  
  - Priority-based scheduling  
  - Round-robin quantum switching  
- Simulated **context switching** and process lifecycle management  

### Process Management  
- Implemented a **Process Control Block (PCB)** architecture  
- Supports **process creation**, **switching**, and **termination**  
- Tracks CPU states, registers, and scheduling metadata  

### Device Management (VFS)  
- Built a **Virtual File System (VFS)** abstraction layer  
- Unified API for `open`, `read`, `write`, `seek` operations  
- Supports both **file-based and random-access devices**

### Interprocess Communication  
- Implemented **asynchronous message-passing** between processes  
- Designed `SendMessage()` and `WaitForMessage()` system calls  
- Enables **userland-level IPC** via kernel-managed queues  

### Memory Management & Paging  
- Simulated a **1MB virtual memory space** with 1KB pages  
- Added **TLB caching** and kernel-level page allocation/freeing  
- Implemented paging algorithms and page table management  

### System Calls  
- Built user–kernel interface for  
  `Sleep()`, `Exit()`, `SendMessage()`, `AllocateMemory()`, and more  
- Ensured seamless **userland ↔ kernelland** transitions  

---

## Technical Highlights

| Category | Details |
|-----------|----------|
| **Language** | Java (JDK 23) |
| **IDE** | IntelliJ IDEA |
| **Architecture** | Object-Oriented, Multi-threaded Simulation |
| **Synchronization** | Java Threads, Semaphores, Timer-based Interrupts |
| **Design Focus** | Process Lifecycle, IPC, Hardware Abstraction |

---

## Achievements
Implemented **90–100%** of all rubric-defined OS features across 5 incremental stages:

1.  Core Kernel & Cooperative Multitasking  
2.  Priority Scheduling and Sleep System Call  
3.  Virtual File System and Device Interface  
4.  Interprocess Communication (Message Passing)  
5.  Paging and Memory Virtualization  

 Achieved full **process isolation**, correct **page mapping**, and consistent **device I/O**.  
 Demonstrated comprehensive understanding of **CPU scheduling**, **memory management**, **IPC**, and **kernel–userland boundaries**.

