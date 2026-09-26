package com.yh.toy_pj.controller;

import com.yh.toy_pj.entity.Ticket;
import com.yh.toy_pj.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TicketController {

    @Autowired
    private TicketRepository ticketRepository;

    @GetMapping("/api/tickets")
    public List<Ticket> getTickets() {
        return ticketRepository.findAll();
    }

    @PostMapping("/api/tickets")
    public Ticket createTicket(@RequestBody Ticket ticket) {
        if (ticket.getStatus() == null || ticket.getStatus().isEmpty()) {
            ticket.setStatus("접수대기");
        }
        return ticketRepository.save(ticket);
    }

    @DeleteMapping("/api/tickets/{id}")
    public void deleteTicket(@PathVariable Long id) {
        ticketRepository.deleteById(id);
    }
}