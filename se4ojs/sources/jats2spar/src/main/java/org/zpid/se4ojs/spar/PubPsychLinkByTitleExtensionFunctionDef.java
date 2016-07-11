package org.zpid.se4ojs.spar;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;

import org.apache.commons.lang3.StringUtils;
import org.zpid.se4ojs.app.Config;
import org.zpid.se4ojs.links.PubPsychApiCaller;


public class PubPsychLinkByTitleExtensionFunctionDef extends ExtensionFunctionDefinition {

	private PubPsychApiCaller pubpsych = new PubPsychApiCaller();

	@Override
	public StructuredQName getFunctionQName() {
		return new StructuredQName("pubpsych", "http://www.zpid.de/pubpsych", "pubpsychLinkByTitle");
	}

	@Override
	public SequenceType[] getArgumentTypes() {
		return new SequenceType[] {
				SequenceType.STRING_SEQUENCE, SequenceType.STRING_SEQUENCE
			};
	}

	@Override
	public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
		return SequenceType.STRING_SEQUENCE;
	}

	@Override
	public ExtensionFunctionCall makeCallExpression() {
		return new ExtensionFunctionCall() {

			@Override public Sequence call(
					XPathContext context, Sequence[] arguments) throws XPathException {
				if (Config.isGeneratePubPsychLinks()) {
					String title = ((StringValue)arguments[0]).asString();
					String firstAuthor = arguments[1].head().getStringValue();
					String result = pubpsych.getPubPsychLinkByTitle(title, firstAuthor);
					StringValue[] result1 = new StringValue[]{StringValue.makeStringValue(result)};
					return new SequenceExtent(result1);
				}
				return new SequenceExtent(
						new StringValue[]{StringValue.makeStringValue(StringUtils.EMPTY)});
			}
		};
	}

}
