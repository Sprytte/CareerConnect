import "../styles/Resume.css";
import Delete from "../assets/trash-icon.svg";
import Edit from "../assets/edit-icon.svg";

type ResumeResponse = {
  id: number;
  userId: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
};

const ResumeItem = ({resume}: {resume: ResumeResponse}) => {
  return (
    <>
      {resume && (
        <div className="resume-selected-file">
          <div>
            <strong>{resume.fileName}</strong>

            <div>
              {(resume.sizeBytes / 1024).toFixed(1)} KB
            </div>

            <div>
              Uploaded:{" "}
              {new Date(
                resume.uploadedAt
              ).toLocaleDateString()}
            </div>
          </div>
          <div className="resume-icons">
            <button className="icon-blue">
              <img src={ Edit } width={18} height={17}/>
            </button>
            <button className="icon-red">
              <img src={ Delete } width={18}/>
            </button>
          </div>
        </div>
      )}
    </>
  );
};

export default ResumeItem;