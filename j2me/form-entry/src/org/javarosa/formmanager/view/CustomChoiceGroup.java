/**
 * 
 */
package org.javarosa.formmanager.view;

import javax.microedition.lcdui.Canvas;

import de.enough.polish.ui.ChoiceGroup;
import de.enough.polish.ui.ChoiceItem;
import de.enough.polish.ui.Container;
import de.enough.polish.ui.Item;
import de.enough.polish.ui.MoreUIAccess;
import de.enough.polish.ui.Style;

/**
 * @author ctsims
 *
 */
public class CustomChoiceGroup extends ChoiceGroup {
	
	private boolean autoSelect;
	protected int lastSelected = -1;
	private int style;
	
	
	//Do this better
	public CustomChoiceGroup(String s, int choiceType, boolean autoSelect) {
		super(s,choiceType);
		this.style = choiceType;
		this.autoSelect = autoSelect;
	}

	public CustomChoiceGroup(String s, int choiceType, boolean autoSelect, Style style) {
		super(s,choiceType, style);
		this.style = choiceType;
		this.autoSelect = autoSelect;
	}
		
		/** Hack #1 & Hack #3**/
		// j2me polish refuses to produce events in the case where select items
		// are already selected. This code intercepts key presses that toggle
		// selection, and clear any existing selection to prevent the supression
		// of the appropriate commands.
		//
		// Hack #3 is due to the fact that multiple choice items won't fire updates
		// unless they haven't been selected by default. The return true here for them
		// ensures that the system knows that fires on multi-select always signify
		// a capture.
		//
		// NOTE: These changes are only necessary for Polish versions > 2.0.5 as far
		// as I can tell. I have tested them on 2.0.4 and 2.0.7 and the changes
		// were compatibly with both
		protected boolean handleKeyReleased(int keyCode, int gameAction) {
			boolean gameActionIsFire = getScreen().isGameActionFire(
					keyCode, gameAction);
			if (gameActionIsFire) {
				ChoiceItem choiceItem = (ChoiceItem) this.focusedItem;
				if(this.choiceType != ChoiceGroup.MULTIPLE) {
					//Hack #1
					choiceItem.isSelected = false;
				}
			}
			boolean superReturn = super.handleKeyReleased(keyCode, gameAction);
			if(gameActionIsFire && this.choiceType == ChoiceGroup.MULTIPLE) {
				//Hack #3
				return true;
			} else {
				return superReturn;
			}
		}
		
		public int getScrollHeight() {
			return super.getScrollHeight();
		}
		
		/** Hack #2 **/
		//This is a slight UI hack that is in place to make the choicegroup properly
		//intercept 'up' and 'down' inputs. Essentially Polish is very broken when it comes
		//to scrolling nested containers, and this function properly takes the parent (Widget) position
		//into account as well as the choicegroup's
		//
		// I have tested this change with Polish 2.0.4 on Nokia Phones and the emulators and it works
		// correctly. It also works correctly on Polish 2.0.7 on the emulator, but I have not attempted
		// to use it on a real nokia phone.
		public int getRelativeScrollYOffset() {
			if (!this.enableScrolling && this.parent instanceof Container) {
				
				// Clayton Sims - Feb 9, 2009 : Had to go through and modify this code again.
				// The offsets are now accumulated through all of the parent containers, not just
				// one.
				Item walker = this.parent;
				int offset = 0;
				
				//Walk our parent containers and accumulate their offsets.
				while(walker instanceof Container) {
					// Clayton Sims - Apr 3, 2009 : 
					// If the container can scroll, it's relativeY is useless, it's in a frame and
					// the relativeY isn't actually applicable.
					// Actually, this should almost certainly _just_ break out of the loop if
					// we hit something that scrolls, but if we have multiple scrolling containers
					// nested, someone _screwed up_.
					if(!MoreUIAccess.isScrollingContainer((Container)walker)) {
						offset += walker.relativeY;
					}
					walker = walker.getParent();
				}
				
				//The value returned here (The + offest part) is the fix.
				int absOffset = ((Container)this.parent).getScrollYOffset() + this.relativeY + offset;
				
				return absOffset;
				
				// Clayton Sims - Feb 10, 2009 : Rolled back because it doesn't work on the 3110c, apparently!
				// Fixing soon.
				//return ((Container)this.parent).getScrollYOffset() + this.relativeY + this.parent.relativeY;
			}
			int offset = this.targetYOffset;
			//#ifdef polish.css.scroll-mode
				if (!this.scrollSmooth) {
					offset = this.yOffset;
				}
			//#endif
			return offset;
		}
		
		/** Hack #4! **/
		//Clayton Sims - Jun 16, 2009
		// Once again we're in the land of hacks.
		// This particular hack is responsible for making it so that when a widget is
		// brought up on the screen, its selected value (if one exists), is currently 
		// highlighted. This vastly improves the back/forward usability.
		// Only Tested on Polish 2.0.4
		public Style focus (Style newStyle, int direction) {
			Style retStyle = super.focus(newStyle, direction);
			if(this.getSelectedIndex() > -1) {
				this.focusChild(this.getSelectedIndex());
			}
			return retStyle; 
		}
		
		
		/** Hack #5 **/
		//Clayton Sims (10/18/2010) These should only happen if the autoSelect isn't true 
		//Anton de Winter 4/23/2010
		//To play an audio file whenever a choice is highlighted we need to make the following hack.
		public void focusChild( int index, Item item, int direction ) {
			if(autoSelect) {
				//skip everything here;
			} else {
				//CTS(10/18/2010) : Remove selectedIndex hack.
				if(direction != 0){
					doAudio(index, false);
				} 
			}
			super.focusChild(index, item, direction);
		}
		
		//This hack handles whether a widget should automatically be advanced
		//when a number is pressed, or whether it should simply change what is
		//currently ready to be selected.
		protected boolean handleKeyPressed(int keyCode, int gameAction){
			
			//Only catch 1-9 and only then when autoselect isn't enabled.
			if(autoSelect || !(keyCode >= Canvas.KEY_NUM1 && keyCode <= Canvas.KEY_NUM9)){
				return super.handleKeyPressed(keyCode,gameAction);
			}else{
				int index = keyCode-Canvas.KEY_NUM1;
				if(index < this.itemsList.size()){
					doAudio(index,true);
					super.focusChild(index);
				}else{
					return super.handleKeyPressed(keyCode,gameAction);
				}
				return true;
			}
		}
		
		private void doAudio(int index, boolean force){
			//We only want to play the audio in certain circumstances to make sure we aren't
			//double playing certain situations.
			if(force || lastSelected != index) {
				lastSelected = index;
				playAudio(index);
			}
		}

	
		public void playAudio(int index) {
			//Nothing, override for custom behavior
		}

		public void setLastSelected(int index) {
			this.lastSelected = index;
		}
}
