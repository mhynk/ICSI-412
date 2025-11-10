#Java Operating System Simulator

A full-featured educational OS implemented from scratch in Java

##Overview

Developed a complete operating system simulator in Java, modeling the core functions of modern OS design.
Implemented process scheduling, device management, interprocess communication, and memory paging using object-oriented architecture within the JVM.

##Key Features

###Kernel & Scheduler – Designed a cooperative multitasking system with priority-based scheduling and round-robin quantum switching.

###Process Management – Built a PCB (Process Control Block) system supporting process creation, switching, and termination.

###Device Management (VFS) – Implemented a Virtual File System to handle abstract device operations (open, read, write, seek) with random and file-based devices.

###Interprocess Communication – Added a message-passing API enabling asynchronous communication between userland processes.

###Memory Management & Paging – Simulated a 1MB virtual memory system with 1KB pages, TLB caching, and kernel-level page allocation/freeing.

###System Calls – Developed APIs (e.g., Sleep(), Exit(), SendMessage(), AllocateMemory()) bridging userland and kernelland interactions.

##Technical Highlights

###Language: Java (JDK 23)

###IDE: IntelliJ IDEA

###Architecture: Object-Oriented, Multi-threaded Simulation

###Synchronization: Java Threads, Semaphores, Timer-based interrupts

###Design Focus: OS process lifecycle, interprocess coordination, and hardware abstraction

##Achievements

Implemented 90–100% of all rubric-defined OS features across 5 incremental stages:

Core Kernel & Cooperative Multitasking

Priority Scheduling and Sleep System Call

Virtual File System and Device Interface

Interprocess Communication (Message Passing)

Paging and Memory Virtualization

Achieved full process isolation, proper page mapping, and consistent I/O device interaction.

Demonstrated comprehensive understanding of CPU scheduling, memory mapping, IPC, and kernel–userland boundaries.
