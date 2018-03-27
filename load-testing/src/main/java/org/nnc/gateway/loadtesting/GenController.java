package org.nnc.gateway.loadtesting;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("gen")
public class GenController {
    @LogMethodCall
    @RequestMapping("string")
    @ResponseBody
    public String string(int len) {
        return StringUtils.repeat('a', len);
    }

    @LogMethodCall
    @RequestMapping("data")
    @ResponseBody
    public Data data() {
        return new Data("Иван Иванов", 35);
    }
}
