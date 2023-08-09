interface TempIdentificationGenerator {
    generate(): string;
}
const usNamesData = {
    usNames: [
        'John',
        'Michael',
        'Emma',
        'Olivia',
        'Liam',
        'Sophia',
        'William',
        'Ava',
        'James',
        'Isabella',
        // Add more names as needed
    ],
};
// Path: src/ts/TempName.ts
class TempIdentificationGeneratorImpl implements TempIdentificationGenerator {
    generate(): string {
        const randomIndex = Math.floor(
            Math.random() * usNamesData.usNames.length,
        );
        return usNamesData.usNames[randomIndex];
    }
}
let tempIdentificationGenerator: TempIdentificationGenerator =
    new TempIdentificationGeneratorImpl();
export { TempIdentificationGenerator, tempIdentificationGenerator };
