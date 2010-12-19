package de.tomsplayground.peanuts.client.widgets;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import de.tomsplayground.peanuts.calculator.Calculator;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class CalculatorText {

	private Text text;

	public CalculatorText(Composite parent, int style) {
		text = new Text(parent, style);
		text.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				calculate();
			}
		});
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR)
					calculate();
			}
		});
	}

	protected void calculate() {
		String t = text.getText();
		if (StringUtils.isNotBlank(t)) {
			try  {
				Calculator calculator = new Calculator();
				calculator.setMathContext(new MathContext(10, RoundingMode.HALF_EVEN));
				BigDecimal result = calculator.parse(t);
				String resultStr = PeanutsUtil.formatCurrency(result, null);
				if (! t.equals(resultStr))
					text.setText(resultStr);
			} catch (RuntimeException e) {
				// Okay
			}
		}
	}

	public Text getText() {
		return text;
	}

}
