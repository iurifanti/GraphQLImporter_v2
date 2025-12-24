/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package graphql.util;

import graphql.GraphQLExcelForm;

/**
 *
 * @author iurif
 */
public class LoggerUI {

    GraphQLExcelForm form;

    private static LoggerUI inst = null;

    private LoggerUI(GraphQLExcelForm form) {
        this.form = form;
    }

    public static void createInstance(GraphQLExcelForm form) {
        inst = new LoggerUI(form);
    }

    public static void log(Object obj) {
        if (obj != null) {
            inst.form.logInfo(obj.toString());
        }
    }
}
