package payback.view;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import payback.params.RuntimeParam;
import payback.params.RuntimeParamsWrapper;

@SuppressWarnings("serial")
public class ParamsPanel extends JPanel {
    private RuntimeParamsWrapper wrapper;

    public ParamsPanel(Object parametrable) {
        this.wrapper = new RuntimeParamsWrapper(parametrable);
        setName("Params: " + parametrable.getClass().getSimpleName());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        final Map<Field, RuntimeParam> params = wrapper.getParams();
        final Map<Field, JTextField> textFieldMap = new HashMap<Field, JTextField>();
        for (Field field : params.keySet()) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            add(panel);
            JLabel label = new JLabel(field.getName());
            panel.add(label);
            JTextField text = new JTextField("" + params.get(field).value());
            panel.add(text);
            textFieldMap.put(field, text);
        }
        JButton button = new JButton("OK");
        add(button);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                for (Field field : params.keySet()) {
                    try {
                        wrapper.setParam(field, Double.parseDouble(textFieldMap.get(field).getText()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        setPreferredSize(new Dimension(500, params.size() * 20 + 40));
    }

}
