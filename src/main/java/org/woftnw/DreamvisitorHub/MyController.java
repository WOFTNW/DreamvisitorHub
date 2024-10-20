package org.woftnw.DreamvisitorHub;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MyController {

    @GetMapping("/message")
    public String getMessage() {
        return "Hello world! This message is from the back-end!";
    }

}
